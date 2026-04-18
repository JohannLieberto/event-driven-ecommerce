package com.ecommerce.apigateway;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiGatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Disabled("End-to-end test: requires order-service running on port 8081")
    void routeToOrderService() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Disabled("End-to-end test: requires inventory-service running on port 8082")
    void routeToInventoryService() {
        webTestClient.get()
                .uri("/api/inventory")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Disabled("End-to-end test: requires full gateway stack")
    void unknownRoute_Returns404() {
        webTestClient.get()
                .uri("/api/unknown")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("No route found for path /api/unknown");
    }
}
