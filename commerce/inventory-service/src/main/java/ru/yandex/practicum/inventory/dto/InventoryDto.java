package ru.yandex.practicum.inventory.dto;

public record InventoryDto(

        Long id,

        Long productId,

        Integer quantity,   // общее количество товара на складе

        Integer reservedQuantity,   // зарезервированное количество

        Integer availableQuantity   //доступное количество, которое вычисляется как quantity - reservedQuantity
) {
}
