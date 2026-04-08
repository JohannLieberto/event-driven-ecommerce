package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(notificationRepository.findByCustomerId(customerId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(notificationRepository.findByOrderId(orderId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("notification-service is running");
    }
}
