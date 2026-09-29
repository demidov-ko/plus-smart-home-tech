package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {
    private final Long productId;

    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super("inventory-service недоступен при резервировании товара id=%d".formatted(productId), cause);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
