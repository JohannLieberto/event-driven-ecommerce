package com.ecommerce.notificationservice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class, EurekaClientAutoConfiguration.class})
@EnableJpaAuditing
@ComponentScan(
        basePackages = "com.ecommerce.notificationservice",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.ecommerce\\.notificationservice\\.config\\.KafkaConfig|com\\.ecommerce\\.notificationservice\\.kafka\\..*"
        )
)
public class TestNotificationServiceApplication {
}
