package ru.yandex.practicum.order.client;

import feign.FeignException;

final class FallbackCause {
    private FallbackCause() { }

    static FeignException findFeignException(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof FeignException feignException) return feignException;
            current = current.getCause();
        }
        return null;
    }
}
