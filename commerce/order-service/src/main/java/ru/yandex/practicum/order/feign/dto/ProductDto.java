package ru.yandex.practicum.order.feign.dto;

import java.math.BigDecimal;

// Такой DTO не делает order-service владельцем данных каталога
// Он только фиксирует, какой ответ сервис заказов ожидает от публичного API product-service
public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean active
) {
}
