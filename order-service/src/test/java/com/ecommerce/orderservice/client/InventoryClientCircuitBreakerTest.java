package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.exception.InventoryServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class InventoryClientCircuitBreakerTest {

    @Autowired
    private InventoryClient inventoryClient;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("inventoryCheckCircuitBreaker").reset();
    }

    @Test
    void checkStock_returnsFalse_whenInventoryServiceDown() {
        when(restTemplate.getForObject(anyString(), eq(InventoryClient.StockCheckResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // First call hits the real exception path -> fallback returns false
        boolean result = inventoryClient.checkStock(1L, 5);
        assertFalse(result, "Fallback should return false when inventory service is unavailable");
    }

    @Test
    void checkStock_circuitOpens_afterRepeatedFailures() {
        when(restTemplate.getForObject(anyString(), eq(InventoryClient.StockCheckResponse.class)))
                .thenThrow(new RuntimeException("Service down"));

        // Drive enough failures to open the circuit (slidingWindowSize=10, threshold=50%)
        for (int i = 0; i < 10; i++) {
            inventoryClient.checkStock(1L, 1);
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("inventoryCheckCircuitBreaker");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState(),
                "Circuit breaker should be OPEN after repeated failures");
    }

    @Test
    void reserveStock_throwsInventoryServiceException_whenCircuitOpen() {
        // Force circuit open
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("inventoryReserveCircuitBreaker");
        cb.transitionToOpenState();

        // Should immediately call fallback and throw InventoryServiceException
        assertThrows(InventoryServiceException.class,
                () -> inventoryClient.reserveStock(1L, 5, 100L));
    }
}
