package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(Long id, Long productId, String productName, Integer quantity,
                           BigDecimal price, BigDecimal lineTotal) {
    public OrderItemDto(Long id, Long productId, String productName, Integer quantity, BigDecimal price) {
        this(id, productId, productName, quantity, price,
                price == null || quantity == null ? null : price.multiply(BigDecimal.valueOf(quantity)));
    }
}
