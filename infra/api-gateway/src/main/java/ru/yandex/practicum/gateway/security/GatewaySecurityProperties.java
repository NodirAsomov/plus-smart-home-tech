package ru.yandex.practicum.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(List<GatewayUser> users) {
    public GatewaySecurityProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record GatewayUser(String username, String password, List<String> roles) {
        public GatewayUser {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
