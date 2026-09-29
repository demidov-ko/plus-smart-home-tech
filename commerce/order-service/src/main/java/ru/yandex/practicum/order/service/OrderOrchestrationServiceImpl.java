package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemData;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOrchestrationServiceImpl implements OrderOrchestrationService {

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    public OrderDto createOrder(CreateOrderRequest request) {
        // Группируем позиции по productId
        Map<Long, Integer> groupedItems = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            groupedItems.merge(item.productId(), item.quantity(), Integer::sum);
        }
        log.info("Сгруппированные позиции заказа: {}", groupedItems);

        // флаги для отслеживания технической деградации
        // если хотя бы один вызов упал в degraded, заказ сохранится как PENDING_CONFIRMATION
        boolean degraded = false;
        String degradationReason = null;

        // Получаем данные товаров (один запрос на уникальный productId)
        Map<Long, ProductDto> products = new HashMap<>();
        for (Long productId : groupedItems.keySet()) {
            // обёртка вызова в ServiceCallResult, который возвращает один из результатов: Success, Failure или Degraded
            ServiceCallResult<ProductDto> result = getProduct(productId);
            switch (result) {
                case ServiceCallResult.Success<ProductDto> s -> {
                    ProductDto product = s.value();
                    if (!product.active()) {
                        throw new OrderProcessingException(
                                "Товар с id=%d снят с продажи".formatted(productId));
                    }
                    products.put(productId, product);
                }
                // бизнес-отказ (товар не найден) - заказ не создаётся
                case ServiceCallResult.Failure<ProductDto> f -> {
                    throw new OrderProcessingException(f.message());
                }
                // техническая деградация - product-service недоступен
                case ServiceCallResult.Degraded<ProductDto> d -> {
                    degraded = true;
                    degradationReason = d.reason();
                    log.warn("Деградация: {}", d.reason());
                    // Не прерываем цикл: остальные товары тоже пытаемся получить,
                    // но они тоже скорее всего упадут в degraded
                }
            }
        }

        // Резервируем товары (суммарное количество по каждому productId)
        List<ReleaseRequest> reserved = new ArrayList<>();
        // пропускаем резервирование, если каталог уже деградировал
        // если product-service недоступен, нет смысла резервировать - данных о товаре нет
        if (!degraded) {
            for (Map.Entry<Long, Integer> entry : groupedItems.entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                // обёртка вызова в ServiceCallResult
                ServiceCallResult<ReserveResponse> result = reserve(productId, quantity);
                switch (result) {
                    case ServiceCallResult.Success<ReserveResponse> s -> {
                        // если успех - запоминаем, что нужно будет снять резерв при откате
                        reserved.add(new ReleaseRequest(productId, quantity));
                        log.info("Зарезервирован товар id={}, количество={}", productId, quantity);
                    }
                    // бизнес-отказ (склад ответил 404/409) - компенсируем и отклоняем
                    case ServiceCallResult.Failure<ReserveResponse> f -> {
                        // если ошибка - снимаем все ранее сделанные резервы
                        compensate(reserved);
                        throw new OrderProcessingException(f.message());
                    }
                    // техническая деградация - inventory-service недоступен
                    case ServiceCallResult.Degraded<ReserveResponse> d -> {
                        degraded = true;
                        degradationReason = d.reason();
                        log.warn("Деградация: {}", d.reason());
                    }
                }
                if (degraded) break;
            }
        }

        // Формируем снимок данных для заказа
        List<OrderItemData> snapshots = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : groupedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductDto product = products.get(productId);

            // если товар не получен из-за деградации каталога
            // сохраняем доступный productId, название-заглушку и цену 0
            if (product != null) {
                snapshots.add(new OrderItemData(
                        productId,
                        product.name(),
                        product.price(),
                        quantity
                ));
            } else {
                snapshots.add(new OrderItemData(
                        productId,
                        "Товар #%d (ожидает проверки)".formatted(productId),
                        BigDecimal.ZERO,
                        quantity
                ));
            }
        }
        // определяем статус заказа
        // Если всё прошло без деградации - CONFIRMED
        // Если хотя бы один сервис был недоступен - PENDING_CONFIRMATION
        OrderStatus status = degraded
                ? OrderStatus.PENDING_CONFIRMATION
                : OrderStatus.CONFIRMED;

        String statusDetails = degraded
                ? "Заказ требует ручной проверки: " + degradationReason
                : null;

        // Сохраняем заказ (в локальной транзакции)
        try {
            return orderService.saveOrder(
                    request.customerName(),
                    request.customerEmail(),
                    snapshots,
                    status,
                    statusDetails
            );
        } catch (Exception e) {
            log.error("Ошибка при сохранении заказа, запускаем компенсацию", e);
            compensate(reserved);
            throw new OrderProcessingException("Не удалось сохранить заказ");
        }
    }

    // обёртка вызова к product-service, возвращающая ServiceCallResult
    private ServiceCallResult<ProductDto> getProduct(Long productId) {
        try {
            // Success — товар получен
            return new ServiceCallResult.Success<>(productClient.getProductById(productId));
        } catch (ProductServiceUnavailableException e) {
            // техническая деградация, Fallback выбросил это исключение — значит product-service технически недоступен
            return new ServiceCallResult.Degraded<>("Каталог временно недоступен");
        } catch (FeignException e) {
            // Бизнес-ошибка (4xx), проброшенная из fallback - это не деградация, а отказ
            return new ServiceCallResult.Failure<>(mapProductException(e, productId).getMessage());
        }
    }

    // обёртка вызова к inventory-service, возвращающая ServiceCallResult
    private ServiceCallResult<ReserveResponse> reserve(Long productId, Integer quantity) {
        try {
            ReserveResponse response = inventoryClient.reserveStock(new ReserveRequest(productId, quantity));
            return new ServiceCallResult.Success<>(response);
        } catch (InventoryServiceUnavailableException e) {
            return new ServiceCallResult.Degraded<>("Склад временно недоступен");
        } catch (FeignException e) {
            return new ServiceCallResult.Failure<>(mapInventoryException(e, productId).getMessage());
        }
    }


    // Преобразует Feign-ошибки от product-service в понятные бизнес-ошибки
    private OrderProcessingException mapProductException(FeignException exception, Long productId) {
        if (exception.status() == 404) {
            return new OrderProcessingException(
                    "Товар с id=%d не найден".formatted(productId)
            );
        }
        return new OrderProcessingException(
                "Не удалось получить данные товара с id=%d".formatted(productId)
        );
    }

    // Преобразует Feign-ошибки от inventory-service в понятные бизнес-ошибки
    private OrderProcessingException mapInventoryException(FeignException exception, Long productId) {
        if (exception.status() == 404) {
            return new OrderProcessingException(
                    "Складская запись для товара id=%d не найдена".formatted(productId));
        }
        if (exception.status() == 409) {
            return new OrderProcessingException(
                    "Недостаточно товара id=%d на складе".formatted(productId));
        }
        return new OrderProcessingException(
                "Не удалось зарезервировать товар id=%d".formatted(productId));
    }

    // Снимает все ранее созданные резервы при срыве сценария
    private void compensate(List<ReleaseRequest> reserved) {
        if (reserved.isEmpty()) return;
        log.warn("Компенсация: снимаем {} резервов", reserved.size());
        for (ReleaseRequest r : reserved) {
            try {
                inventoryClient.releaseStock(r);
                log.info("Снят резерв для товара id={}, количество={}", r.productId(), r.quantity());
            } catch (Exception e) {
                log.error("Не удалось снять резерв для товара id={}", r.productId(), e);
            }
        }
    }
}
