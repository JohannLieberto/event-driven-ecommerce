package com.example.shipping_service.service;

import com.example.shipping_service.entity.Shipment;
import com.example.shipping_service.event.PaymentProcessedEvent;
import com.example.shipping_service.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    public void scheduleShipment(PaymentProcessedEvent event) {

        Shipment shipment = new Shipment();
        shipment.setOrderId(event.getOrderId());
        shipment.setStatus("SCHEDULED");

        shipmentRepository.save(shipment);

        System.out.println("Shipment scheduled for order: " + event.getOrderId());
    }
}