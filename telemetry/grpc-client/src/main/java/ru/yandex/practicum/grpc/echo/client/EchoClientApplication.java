package ru.yandex.practicum.grpc.echo.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EchoClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(EchoClientApplication.class, args);
    }
}
