package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.InventoryClientPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderServiceApplicationTests {

    @MockBean
    private InventoryClientPort inventoryClient;

    @Test
    void contextLoads() {
        // Verifies the Spring context loads with test config from src/test/resources/application.yml
    }
}
