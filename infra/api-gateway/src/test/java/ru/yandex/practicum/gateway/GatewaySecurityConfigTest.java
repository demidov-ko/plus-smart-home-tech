package ru.yandex.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogGet_isPublic() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        webTestClient.post().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_passesSecurity() {
        webTestClient.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().value(this::assertPassedSecurity);
    }

    @Test
    void productWrite_withUserCredentials_isForbidden() {
        webTestClient.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdminCredentials_passesSecurity() {
        webTestClient.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().value(this::assertPassedSecurity);
    }

    @Test
    void allOrders_withUserCredentials_isForbidden() {
        webTestClient.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void allOrders_withAdminCredentials_passesSecurity() {
        webTestClient.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().value(this::assertPassedSecurity);
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get().uri("/api/unknown")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void corsPreflight_isPublic() {
        webTestClient.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().value(this::assertPassedSecurity);
    }

    private void assertPassedSecurity(int status) {
        if (status == 401 || status == 403) {
            throw new AssertionError(
                    "Ожидали, что запрос пройдёт security-проверку, а получили статус " + status);
        }
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(path("/test-backend"), request -> ServerResponse.ok().build());
        }

        // Подменяем реальные маршруты Gateway (которые в тесте и не загрузятся,
        // так как Config Server отключён) на простой форвард в /test-backend -
        // ровно для тех префиксов, что фигурируют в тестах выше.
        @Bean
        RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-products", r -> r.path("/api/products/**").uri("forward:/test-backend"))
                    .route("test-orders", r -> r.path("/api/orders/**").uri("forward:/test-backend"))
                    .build();
        }
    }
}

