package com.ecommerce.shippingservice.controller;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipment> getShipmentByOrderId(@PathVariable Long orderId) {
        return shipmentRepository.findFirstByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Shipment>> getShipmentsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(shipmentRepository.findByCustomerId(customerId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("shipping-service is running");
    }
}
