package ru.yandex.practicum.order.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {
    @Bean Logger.Level feignLoggerLevel() { return Logger.Level.BASIC; }
    @Bean RequestInterceptor sourceServiceHeader() { return template -> template.header("X-Source-Service", "order-service"); }
}
