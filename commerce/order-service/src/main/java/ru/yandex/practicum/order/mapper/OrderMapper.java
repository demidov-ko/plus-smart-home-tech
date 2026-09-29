package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.dto.OrderItemData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getPrice()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreatedAt(),
                items
        );
    }

    public Order toEntity(String customerName, String customerEmail,
                          List<OrderItemData> items,
                          OrderStatus status, String statusDetails) {
        Order order = Order.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .status(status) // берем из параметра
                .statusDetails(statusDetails)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemData item : items) {
            // создаём сущность позиции и копируем productId, productName, quantity, price
            // это «снимок» данных товара на момент оформления заказа, чтобы заказ не пострадал при изм. товара
            OrderItem entity = OrderItem.builder()
                    .productId(item.productId())
                    .productName(item.productName())
                    .quantity(item.quantity())
                    .price(item.price())
                    .build();

            order.addItem(entity);

            // .multiply() - умножение BigDecimal (для денег нельзя использовать оператор *)
            // total.add(...) - прибавляем к общей сумме (BigDecimal изменять нельзя, add() возвращает новый)
            total = total.add(item.price().multiply(BigDecimal.valueOf(item.quantity())));
        }

        order.setTotalPrice(total);
        return order;
    }
}
