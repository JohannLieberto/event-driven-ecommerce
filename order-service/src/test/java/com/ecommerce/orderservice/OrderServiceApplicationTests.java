package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.InventoryClientPort;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderServiceApplicationTests {

    @MockBean
    private InventoryClientPort inventoryClient;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void contextLoads() {
        // Verifies the Spring context loads correctly with test configuration from application.yml
    }
}
