package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);
    @Override public ProductClient create(Throwable cause) {
        return productId -> {
            FeignException feign = FallbackSupport.feignCause(cause);
            log.warn("product-service failed while loading product {}: {}", productId, cause.toString());
            if (feign != null && feign.status() == 404) throw new OrderProcessingException("Товар не найден: " + productId);
            if (feign != null && feign.status() >= 400 && feign.status() < 500)
                throw new OrderProcessingException("Каталог отклонил обработку товара " + productId);
            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}
