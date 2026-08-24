package ru.yandex.practicum.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull(message = "Product id is required") Long productId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {
    @JsonIgnore
    public OrderItemRequest(Long productId, String ignoredName, Integer quantity, BigDecimal ignoredPrice) {
        this(productId, quantity);
    }
}
