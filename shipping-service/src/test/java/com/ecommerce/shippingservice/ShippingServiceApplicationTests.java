package com.ecommerce.shippingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false"
        }
)
@ActiveProfiles("test")
class ShippingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
