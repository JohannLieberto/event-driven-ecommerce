package com.ecommerce.apigateway.controller;

import com.ecommerce.apigateway.dto.AuthResponse;
import com.ecommerce.apigateway.dto.LoginRequest;
import com.ecommerce.apigateway.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request) {

        // Simple validation (in real app, check against database)
        if (validateCredentials(request.getUsername(), request.getPassword())) {
            List<String> roles = Arrays.asList("ROLE_USER");
            String token = tokenProvider.generateToken(request.getUsername(), roles);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUsername(request.getUsername());
            response.setExpiresIn(86400); // 24 hours in seconds

            return Mono.just(ResponseEntity.ok(response));
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }

    private boolean validateCredentials(String username, String password) {
        // Hardcoded users for demo (in production, check database)
        Map<String, String> users = Map.of(
                "customer1", "pass123",
                "admin", "admin123"
        );

        return users.containsKey(username) &&
                users.get(username).equals(password);
    }
}