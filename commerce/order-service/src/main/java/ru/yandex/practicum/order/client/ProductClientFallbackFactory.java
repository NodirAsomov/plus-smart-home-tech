package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            FeignException feignException = FallbackCause.findFeignException(cause);
            if (feignException != null && feignException.status() == 404) throw feignException;
            log.warn("Product service call failed while loading product {}: {}", productId, cause.toString());
            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}
