package com.ecommerce.apigateway;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.annotations.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApiGatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void routeToOrderService() {
        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void routeToInventoryService() {
        webTestClient.get()
                .uri("/api/inventory")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_Returns404() {
        webTestClient.get()
                .uri("/api/unknown")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("No route found for path /api/unknown");
    }
}