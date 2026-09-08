package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {
    public InventoryServiceUnavailableException(String action, Long productId, Throwable cause) {
        super("Inventory service is unavailable while " + action + " product " + productId, cause);
    }
}
