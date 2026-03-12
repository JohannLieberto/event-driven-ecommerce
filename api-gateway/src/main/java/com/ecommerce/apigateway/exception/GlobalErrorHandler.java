package com.ecommerce.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
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
            return writeErrorResponse(response,
                    "No route found for path: " + exchange.getRequest().getPath());
        }

        if (ex instanceof ConnectException) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return writeErrorResponse(response, "Service temporarily unavailable");
        }

        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode statusCode = responseStatusException.getStatusCode();
            int statusValue = statusCode.value();
            response.setStatusCode(statusCode);
            return writeErrorResponse(response, responseStatusException.getReason());
        }

        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return writeErrorResponse(response, "Internal server error");
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, String message) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatusCode statusCode = response.getStatusCode();
        int statusValue = statusCode != null ? statusCode.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                statusValue,
                message,
                null
        );

        byte[] bytes;
        try {
            bytes = new ObjectMapper().writeValueAsBytes(error);
        } catch (JsonProcessingException e) {
            bytes = "{\"message\":\"Internal error\"}".getBytes();
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
