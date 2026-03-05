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
import com.ecommerce.apigateway.exception.ErrorResponse;
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
                throw new RuntimeException(e);
            }
        }

        if (ex instanceof ConnectException) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            try {
                return writeErrorResponse(response,
                        "Service temporarily unavailable");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            return writeErrorResponse(response, "Internal server error");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, String message) throws JsonProcessingException {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                response.getStatusCode().value(),
                message
        );

        byte[] bytes = new ObjectMapper().writeValueAsBytes(error);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}