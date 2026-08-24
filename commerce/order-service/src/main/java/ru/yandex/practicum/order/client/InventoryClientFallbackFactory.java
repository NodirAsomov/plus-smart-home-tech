package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.dto.InventoryRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    private static final Logger log = LoggerFactory.getLogger(InventoryClientFallbackFactory.class);
    @Override public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override public ru.yandex.practicum.order.client.dto.InventoryResponse reserve(InventoryRequest request) {
                throw classify(request.productId(), "reserving", cause);
            }
            @Override public ru.yandex.practicum.order.client.dto.InventoryResponse release(InventoryRequest request) {
                throw classify(request.productId(), "releasing", cause);
            }
        };
    }
    private RuntimeException classify(Long productId, String action, Throwable cause) {
        log.warn("inventory-service failed while {} product {}: {}", action, productId, cause.toString());
        FeignException feign = FallbackSupport.feignCause(cause);
        if (feign != null && feign.status() == 404) return new OrderProcessingException("Складская запись не найдена для товара " + productId);
        if (feign != null && feign.status() == 409) return new OrderProcessingException("Недостаточно товара на складе: " + productId);
        if (feign != null && feign.status() >= 400 && feign.status() < 500) return new OrderProcessingException("Склад отклонил операцию для товара " + productId);
        return new InventoryServiceUnavailableException(productId, action, cause);
    }
}
