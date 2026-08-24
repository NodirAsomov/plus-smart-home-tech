package ru.yandex.practicum.order.client.dto;
import java.math.BigDecimal;
public record ProductResponse(Long id, String name, BigDecimal price, Boolean active) { }
