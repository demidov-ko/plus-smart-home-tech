package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;

//  Это промежуточный DTO, который используют между оркестратором и сервисом заказов
public record OrderItemData(
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity
) {}

