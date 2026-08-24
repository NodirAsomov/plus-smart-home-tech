package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {
    public InventoryServiceUnavailableException(Long productId, String action, Throwable cause) {
        super("Inventory service unavailable while " + action + " product " + productId, cause);
    }
}
