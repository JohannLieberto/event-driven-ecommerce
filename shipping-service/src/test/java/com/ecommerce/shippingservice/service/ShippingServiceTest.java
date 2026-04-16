package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.event.PaymentCompletedEvent;
import com.ecommerce.shippingservice.kafka.ShippingEventPublisher;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShippingServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShippingEventPublisher shippingEventPublisher;

    @InjectMocks
    private ShippingService shippingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void scheduleShipment_success_publishesShipmentEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(1L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_SUCCESS");
        event.setTransactionId("txn-abc");

        when(shipmentRepository.findFirstByOrderId(1L)).thenReturn(Optional.empty());

        Shipment savedShipment = new Shipment();
        savedShipment.setId(1L);
        savedShipment.setOrderId(1L);
        savedShipment.setCustomerId(100L);
        savedShipment.setStatus("SHIPMENT_SCHEDULED");
        savedShipment.setTrackingNumber("TRK-ABCD1234");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(savedShipment);
        doNothing().when(shippingEventPublisher).publishShipmentScheduled(any());

        shippingService.scheduleShipment(event);

        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(shippingEventPublisher, times(1)).publishShipmentScheduled(any());
    }

    @Test
    void scheduleShipment_paymentFailed_skipsShipment() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(2L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_FAILED");

        when(shipmentRepository.findFirstByOrderId(2L)).thenReturn(Optional.empty());

        shippingService.scheduleShipment(event);

        verify(shipmentRepository, never()).save(any());
        verify(shippingEventPublisher, never()).publishShipmentScheduled(any());
    }

    @Test
    void scheduleShipment_idempotent_skipsIfAlreadyScheduled() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(3L);
        event.setCustomerId(100L);
        event.setStatus("PAYMENT_SUCCESS");

        Shipment existing = new Shipment();
        existing.setOrderId(3L);
        when(shipmentRepository.findFirstByOrderId(3L)).thenReturn(Optional.of(existing));

        shippingService.scheduleShipment(event);

        verify(shipmentRepository, never()).save(any());
        verify(shippingEventPublisher, never()).publishShipmentScheduled(any());
    }
}
