package com.ecommerce.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.WebFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.ecommerce.apigateway.exception.ErrorResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.server.WebFilterChain;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    // Paths that do NOT require a JWT token.
    // NOTE: /api/** is open because this is an internal dev environment without auth.
    // Spring Security's SecurityConfig also permits /api/**, but this filter runs
    // before Spring Security — so it must be explicitly listed here too.
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/actuator/health",
            "/actuator/info",
            "/eureka",
            "/api/",
            "/public/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip JWT check for public paths
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid authorization header",
                    HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // Validate token
        if (!tokenProvider.validateToken(token)) {
            return onError(exchange, "Invalid or expired token",
                    HttpStatus.UNAUTHORIZED);
        }

        // Extract username and forward in header
        String username = tokenProvider.getUsernameFromToken(token);
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", username)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message,
                               HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                message,
                null
        );

        byte[] bytes;
        try {
            bytes = new ObjectMapper().writeValueAsBytes(error);
        } catch (Exception e) {
            bytes = "{\"message\":\"Internal error\"}".getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
