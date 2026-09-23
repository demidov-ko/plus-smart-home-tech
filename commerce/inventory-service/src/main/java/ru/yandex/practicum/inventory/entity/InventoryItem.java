package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    // optimistic locking: защищает от потери параллельных изменений при одновременном резервировании товара
    // В таблице будет доп. колонка с версией
    // при inventoryRepository.save(item) происходит автоматическая проверка версии,
    // а в случае ошибки вылетит ObjectOptimisticLockingFailureException
    @Version
    private Long version;
}
