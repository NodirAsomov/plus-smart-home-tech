package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {ru.yandex.practicum.gateway.ApiGateway.class, GatewaySecurityConfigTest.TestBackendConfig.class})
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {
    @Autowired WebTestClient client;

    @Test void catalogGetIsPublic() { get("/api/products").exchange().expectStatus().isOk(); }
    @Test void orderCreateWithoutCredentialsIsUnauthorized() { post("/api/orders").exchange().expectStatus().isUnauthorized(); }
    @Test void orderCreateWithUserCredentialsPasses() { post("/api/orders").header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan")).exchange().expectStatus().isOk(); }
    @Test void productWriteWithUserCredentialsIsForbidden() { post("/api/products").header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan")).exchange().expectStatus().isForbidden(); }
    @Test void productWriteWithAdminCredentialsPasses() { post("/api/products").header(HttpHeaders.AUTHORIZATION, basic("anna", "anna")).exchange().expectStatus().isOk(); }
    @Test void ordersListWithUserCredentialsIsForbidden() { get("/api/orders").header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan")).exchange().expectStatus().isForbidden(); }
    @Test void ordersListWithAdminCredentialsPasses() { get("/api/orders").header(HttpHeaders.AUTHORIZATION, basic("anna", "anna")).exchange().expectStatus().isOk(); }
    @Test void unknownRouteWithAdminCredentialsIsForbidden() { get("/api/unknown").header(HttpHeaders.AUTHORIZATION, basic("anna", "anna")).exchange().expectStatus().isForbidden(); }
    @Test void corsPreflightIsPublic() {
        client.method(HttpMethod.OPTIONS).uri("http://localhost/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange().expectStatus().isOk();
    }

    private WebTestClient.RequestHeadersSpec<?> get(String path) { return client.get().uri(path); }
    private WebTestClient.RequestHeadersSpec<?> post(String path) { return client.post().uri(path); }
    private String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {
        @Bean RouterFunction<ServerResponse> testBackendRoutes() {
            return route(path("/api/products/**"), request -> ServerResponse.ok().build())
                    .andRoute(path("/api/orders/**"), request -> ServerResponse.ok().build());
        }

        @Bean CorsWebFilter corsWebFilter() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of("http://localhost:3000"));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return new CorsWebFilter(source);
        }
    }
}
