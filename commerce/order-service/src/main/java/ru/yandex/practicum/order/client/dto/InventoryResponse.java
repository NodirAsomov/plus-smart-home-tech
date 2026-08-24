package ru.yandex.practicum.order.client.dto;

public record InventoryResponse(Boolean success, Integer availableQuantity, String message) { }
