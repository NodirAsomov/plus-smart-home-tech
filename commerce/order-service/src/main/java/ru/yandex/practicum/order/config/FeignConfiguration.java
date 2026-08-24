package ru.yandex.practicum.order.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;

@Configuration
public class FeignConfiguration {
    @Bean Logger.Level feignLoggerLevel() { return Logger.Level.BASIC; }
    @Bean RequestInterceptor requestIdInterceptor() {
        return template -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            String requestId = attributes instanceof ServletRequestAttributes servlet
                    ? servlet.getRequest().getHeader("X-Request-Id") : null;
            template.header("X-Request-Id", requestId == null || requestId.isBlank()
                    ? UUID.randomUUID().toString() : requestId);
            template.header("X-Source-Service", "order-service");
        };
    }
}
