package ru.yandex.practicum.order.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReserveResponse(
        Long productId,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
