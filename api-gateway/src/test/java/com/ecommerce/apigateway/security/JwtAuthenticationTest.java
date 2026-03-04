package com.ecommerce.apigateway.security;

import com.ecommerce.apigateway.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testng.annotations.Test;

import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
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

        String token = tokenProvider.generateToken(
                "customer1",
                Arrays.asList("ROLE_USER")
        );

        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_ReturnsUnauthorized() {

        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Missing or invalid authorization header");
    }

    @Test
    void accessProtectedEndpoint_WithInvalidToken_ReturnsUnauthorized() {

        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Invalid or expired token");
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