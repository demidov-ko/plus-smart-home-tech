package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemData;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.*;

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

        // Получаем данные товаров (один запрос на уникальный productId)
        Map<Long, ProductDto> products = new HashMap<>();
        for (Long productId : groupedItems.keySet()) {
            ProductDto product;
            try {
                product = productClient.getProductById(productId);
            } catch (FeignException e) {
                throw mapProductException(e, productId);
            }
            if (!product.active()) {
                throw new OrderProcessingException(
                        "Товар с id=%d снят с продажи".formatted(productId));
            }
            products.put(productId, product);
        }

        // Резервируем товары (суммарное количество по каждому productId)
        List<ReleaseRequest> reserved = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : groupedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            try {
                // Пытаемся зарезервировать товар на удалённом сервисе
                inventoryClient.reserveStock(new ReserveRequest(productId, quantity));
                // Если успех - запоминаем, что нужно будет снять резерв при откате
                reserved.add(new ReleaseRequest(productId, quantity));
                log.info("Зарезервирован товар id={}, количество={}", productId, quantity);
            } catch (FeignException e) {
                // Если ошибка — снимаем все ранее сделанные резервы
                compensate(reserved);
                throw mapInventoryException(e, productId);
            }
        }

        // Формируем снимок данных для заказа
        List<OrderItemData> snapshots = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : groupedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductDto product = products.get(productId);

            snapshots.add(new OrderItemData(
                    productId,
                    product.name(),
                    product.price(),
                    quantity
            ));
        }

        // Сохраняем заказ (в локальной транзакции)
        try {
            return orderService.saveOrder(
                    request.customerName(),
                    request.customerEmail(),
                    snapshots
            );
        } catch (Exception e) {
            log.error("Ошибка при сохранении заказа, запускаем компенсацию", e);
            compensate(reserved);
            throw new OrderProcessingException("Не удалось сохранить заказ");
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
