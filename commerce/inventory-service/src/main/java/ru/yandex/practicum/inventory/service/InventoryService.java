package ru.yandex.practicum.inventory.service;

import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;

import java.util.List;

public interface InventoryService {

    List<InventoryDto> getAll();

    InventoryDto getByProductId(Long productId);

    InventoryItem getEntityByProductId(Long productId);

    InventoryDto create(UpdateInventoryRequest request);

    InventoryDto updateQuantity(UpdateInventoryRequest request);

    ReserveResponse reserve(ReserveRequest request);
}
