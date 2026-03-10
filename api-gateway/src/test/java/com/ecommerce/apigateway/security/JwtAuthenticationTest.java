package com.ecommerce.apigateway.security;

import com.ecommerce.apigateway.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void login_ValidCredentials_ReturnsToken() {

        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("pass123");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").exists()
                .jsonPath("$.username").isEqualTo("customer1")
                .jsonPath("$.expiresIn").isEqualTo(86400);
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() {

        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("wrongpass");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessProtectedEndpoint_WithValidToken_Success() {

        // Generate a valid token directly — no login call needed
        String token = tokenProvider.generateToken(
                "customer1",
                Arrays.asList("ROLE_USER")
        );

        // The gateway will accept the token but downstream is not running,
        // so we expect either 200 (if downstream mock responds) or 503
        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                status == 200 || status == 503,
                                "Expected 200 or 503 but got: " + status
                        ));
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_ReturnsUnauthorized() {

        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessProtectedEndpoint_WithInvalidToken_ReturnsUnauthorized() {

        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessPublicEndpoint_WithoutToken_Success() {

        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("pass123");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }
}
