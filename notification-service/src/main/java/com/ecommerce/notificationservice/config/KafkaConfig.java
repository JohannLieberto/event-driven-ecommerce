package com.ecommerce.notificationservice.config;

import com.ecommerce.notificationservice.event.InventoryReservedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConfig(String groupId, Class<?> valueType) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return config;
    }

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConfig("notification-service-payment-group", PaymentCompletedEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ShipmentScheduledEvent> shipmentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConfig("notification-service-shipment-group", ShipmentScheduledEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ShipmentScheduledEvent> shipmentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShipmentScheduledEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(shipmentConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, InventoryReservedEvent> inventoryConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConfig("notification-service-inventory-group", InventoryReservedEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> inventoryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryConsumerFactory());
        return factory;
    }
}
