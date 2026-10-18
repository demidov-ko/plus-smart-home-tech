package ru.yandex.practicum.order.feign.dto;

public record ReleaseRequest(
        Long productId,
        Integer quantity
) {}
