package ru.yandex.practicum.order.client;

import feign.FeignException;

final class FallbackSupport {
    private FallbackSupport() { }
    static FeignException feignCause(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof FeignException exception) return exception;
            current = current.getCause();
        }
        return null;
    }
}
