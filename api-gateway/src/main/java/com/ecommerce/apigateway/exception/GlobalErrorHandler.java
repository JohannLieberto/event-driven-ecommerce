package com.ecommerce.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.LocalDateTime;

@Component
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (ex instanceof NotFoundException) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            try {
                return writeErrorResponse(response,
                        "No route found for path: " + exchange.getRequest().getPath());
            } catch (JsonProcessingException e) {
                return writeErrorFallback(response);
            }
        }

        if (ex instanceof ConnectException) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            try {
                return writeErrorResponse(response, "Service temporarily unavailable");
            } catch (JsonProcessingException e) {
                return writeErrorFallback(response);
            }
        }

        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            return writeErrorResponse(response, "Internal server error");
        } catch (JsonProcessingException e) {
            return writeErrorFallback(response);
        }
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, String message) throws JsonProcessingException {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        HttpStatus status = response.getStatusCode() != null
                ? HttpStatus.resolve(response.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        int statusCode = status != null ? status.value() : 500;
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                statusCode,
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

    private Mono<Void> writeErrorFallback(ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"message\":\"Internal error\"}".getBytes();
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
