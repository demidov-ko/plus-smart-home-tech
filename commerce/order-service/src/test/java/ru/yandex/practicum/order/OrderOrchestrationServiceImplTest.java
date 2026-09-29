package ru.yandex.practicum.order;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemData;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.service.OrderOrchestrationServiceImpl;
import ru.yandex.practicum.order.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceImplTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderOrchestrationServiceImpl orchestrationService;

    // Успешное создание заказа (CONFIRMED)
    @Test
    void shouldCreateOrderAsConfirmedWhenAllServicesAvailable() {
        // product-service возвращает активный товар
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(1L, "Умная лампа", "Описание",
                        new BigDecimal("3490.00"), true));
        // inventory-service успешно резервирует
        when(inventoryClient.reserveStock(any(ReserveRequest.class)))
                .thenReturn(null);
        // saveOrder возвращает созданный заказ
        when(orderService.saveOrder(anyString(), anyString(), any(), any(), any()))
                .thenReturn(new OrderDto(1L, "Иван Петров", "petrov@mail,ru",
                        "CONFIRMED", new BigDecimal("6980.00"), null,
                        LocalDateTime.now(), List.of()));

        // Вызываем оркестратор
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван Петров", "petrov@mail,ru",
                List.of(new OrderItemRequest(1L, 2)));

        orchestrationService.createOrder(request);

        // Перехватываем аргументы, переданные в saveOrder
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemData>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);

        verify(orderService).saveOrder(
                eq("Иван Петров"), eq("petrov@mail,ru"),
                itemsCaptor.capture(),
                statusCaptor.capture(),
                detailsCaptor.capture());

        // Статус - CONFIRMED, детали пустые
        assertThat(statusCaptor.getValue()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(detailsCaptor.getValue()).isNull();
        // Позиция - реальные данные товара, а не заглушка
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).productName()).isEqualTo("Умная лампа");
        assertThat(itemsCaptor.getValue().get(0).price()).isEqualByComparingTo("3490.00");
    }

    // Бизнес-ошибка - товар не найден
    @Test
    void shouldRejectOrderWhenProductNotFound() {
        // product-service отвечает 404 - пробрасывается как FeignException
        when(productClient.getProductById(1L))
                .thenThrow(feignException(404));

        CreateOrderRequest request = new CreateOrderRequest(
                "Иван Петров", "petrov@mail,ru",
                List.of(new OrderItemRequest(1L, 2)));

        assertThatThrownBy(() -> orchestrationService.createOrder(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найден");

        // Заказ не должен быть сохранён
        verify(orderService, never())
                .saveOrder(any(), any(), any(), any(), any());
        // Резервирование тоже не должно было произойти
        verify(inventoryClient, never()).reserveStock(any());
    }

    // Техническая деградация product-service
    @Test
    void shouldCreateOrderAsPendingWhenProductServiceUnavailable() {
        // product-service технически недоступен - fallback выбросил типизированное исключение
        when(productClient.getProductById(1L))
                .thenThrow(new ProductServiceUnavailableException(1L, new RuntimeException("timeout")));

        when(orderService.saveOrder(anyString(), anyString(), any(), any(), any()))
                .thenReturn(new OrderDto(1L, "Иван Петров", "petrov@mail,ru",
                        "PENDING_CONFIRMATION", BigDecimal.ZERO,
                        "Заказ требует ручной проверки: Каталог временно недоступен",
                        LocalDateTime.now(), List.of()));

        CreateOrderRequest request = new CreateOrderRequest(
                "Иван Петров", "petrov@mail,ru",
                List.of(new OrderItemRequest(1L, 2)));

        orchestrationService.createOrder(request);

        // Перехватываем аргументы
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemData>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);

        verify(orderService).saveOrder(
                eq("Иван Петров"), eq("petrov@mail,ru"),
                itemsCaptor.capture(),
                statusCaptor.capture(),
                detailsCaptor.capture());

        assertThat(statusCaptor.getValue()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        // Детали содержат причину деградации
        assertThat(detailsCaptor.getValue()).contains("ручной проверки");
        assertThat(detailsCaptor.getValue()).contains("Каталог временно недоступен");
        // Товар - заглушка: название с пометкой, цена 0
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).productName())
                .isEqualTo("Товар #1 (ожидает проверки)");
        assertThat(itemsCaptor.getValue().get(0).price())
                .isEqualByComparingTo(BigDecimal.ZERO);
        // Резервирование не должно было произойти (degraded = true пропускает цикл)
        verify(inventoryClient, never()).reserveStock(any());
    }

    // Частичная деградация
    @Test
    void shouldCreateOrderAsPendingWhenOneProductDegraded() {
        // Первый товар - успешно
        when(productClient.getProductById(1L))
                .thenReturn(new ProductDto(1L, "Умная лампа", "Описание",
                        new BigDecimal("3490.00"), true));
        // Второй товар - техническая деградация
        when(productClient.getProductById(2L))
                .thenThrow(new ProductServiceUnavailableException(2L, new RuntimeException("timeout")));

        when(orderService.saveOrder(anyString(), anyString(), any(), any(), any()))
                .thenReturn(new OrderDto(1L, "Иван Петров", "petrov@mail,ru",
                        "PENDING_CONFIRMATION", new BigDecimal("6980.00"),
                        "Заказ требует ручной проверки",
                        LocalDateTime.now(), List.of()));

        // Запрос с двумя товарами
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван Петров", "petrov@mail,ru",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                ));

        orchestrationService.createOrder(request);

        // Перехватываем аргументы
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemData>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);

        verify(orderService).saveOrder(
                eq("Иван Петров"), eq("petrov@mail,ru"),
                itemsCaptor.capture(),
                statusCaptor.capture(),
                any());

        // Статус — PENDING_CONFIRMATION (достаточно одной деградации)
        assertThat(statusCaptor.getValue()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);

        // Две позиции в заказе
        List<OrderItemData> items = itemsCaptor.getValue();
        assertThat(items).hasSize(2);

        // Первый товар - реальные данные
        assertThat(items.get(0).productId()).isEqualTo(1L);
        assertThat(items.get(0).productName()).isEqualTo("Умная лампа");
        assertThat(items.get(0).price()).isEqualByComparingTo("3490.00");

        // Второй товар - заглушка
        assertThat(items.get(1).productId()).isEqualTo(2L);
        assertThat(items.get(1).productName()).isEqualTo("Товар #2 (ожидает проверки)");
        assertThat(items.get(1).price()).isEqualByComparingTo(BigDecimal.ZERO);

        // Резервирование не должно было произойти (degraded = true)
        verify(inventoryClient, never()).reserveStock(any());
    }


    // FeignException с нужным HTTP-статусом
    private FeignException feignException(int status) {
        return new FeignException(status, "Test exception") {
            @Override
            public int status() {
                return status;
            }
        };
    }
}
