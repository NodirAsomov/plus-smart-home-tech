package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.dto.InventoryRequest;
import ru.yandex.practicum.order.client.dto.InventoryResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    private static final Logger log = LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public InventoryResponse reserve(InventoryRequest request) {
                rethrowBusinessFailure(cause);
                log.warn("Inventory service call failed while reserving product {}: {}", request.productId(), cause.toString());
                throw new InventoryServiceUnavailableException("reserving", request.productId(), cause);
            }

            @Override
            public InventoryResponse release(InventoryRequest request) {
                rethrowBusinessFailure(cause);
                log.warn("Inventory service call failed while releasing product {}: {}", request.productId(), cause.toString());
                throw new InventoryServiceUnavailableException("releasing", request.productId(), cause);
            }
        };
    }

    private static void rethrowBusinessFailure(Throwable cause) {
        FeignException feignException = FallbackCause.findFeignException(cause);
        if (feignException != null && (feignException.status() == 404 || feignException.status() == 409)) {
            throw feignException;
        }
    }
}
