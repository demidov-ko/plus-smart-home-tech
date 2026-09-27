package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InvalidStateException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream()
                .map(inventoryMapper::toDto)
                .toList();
    }

    @Override
    public InventoryDto getByProductId(Long productId) {
        return inventoryMapper.toDto(getEntityByProductId(productId));
    }

    @Override
    public InventoryItem getEntityByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись для товара с id=" + productId + " не найдена"));
    }

    @Override
    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InvalidStateException(
                    "Складская запись для товара с id=" + request.productId() + " уже существует");
        }

        if (request.quantity() < 0) {
            throw new IllegalArgumentException("Количество не может быть отрицательным");
        }

        InventoryItem item = inventoryMapper.toEntity(request);
        return inventoryMapper.toDto(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryDto updateQuantity(UpdateInventoryRequest request) {
        if (request.quantity() < 0) {
            throw new IllegalArgumentException("Количество не может быть отрицательным");
        }

        InventoryItem item = getEntityByProductId(request.productId());

        if (request.quantity() < item.getReservedQuantity()) {
            throw new InvalidStateException(
                    "Новое количество (" + request.quantity() + ") меньше зарезервированного ("
                            + item.getReservedQuantity() + ") для товара id=" + request.productId());
        }

        item.setQuantity(request.quantity());
        return inventoryMapper.toDto(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Количество для резерва должно быть положительным");
        }

        InventoryItem item = getEntityByProductId(request.productId());

        if (item.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    "Недостаточно товара на складе: доступно " + item.getAvailableQuantity()
                            + ", запрошено " + request.quantity());
        }

        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        InventoryItem saved = inventoryRepository.save(item);

        return new ReserveResponse(true, saved.getAvailableQuantity(), "Товар успешно зарезервирован");
    }
}

