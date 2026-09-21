package ru.yandex.practicum.inventory.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;

@Component
public class InventoryMapper {

    public InventoryDto toDto(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return new InventoryDto(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity()
        );
    }

    public InventoryItem toEntity(UpdateInventoryRequest request) {
        if (request == null) {
            return null;
        }
        return InventoryItem.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .build();
    }
}
