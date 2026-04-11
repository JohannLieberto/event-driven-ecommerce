package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.EmailSenderService;
import com.ecommerce.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailSenderService emailSenderService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(notificationService.getNotificationsByCustomerId(customerId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("notification-service is running");
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        emailSenderService.sendEmail(
                "test@test.com",
                "Test Subject",
                "This is a test notification email"
        );
        return ResponseEntity.ok("Test email triggered successfully");
    }

    @PostMapping("/test-payment")
    public ResponseEntity<String> testPaymentFlow() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(9001L);
        event.setCustomerId(1L);
        event.setEmail("test@test.com");
        event.setStatus("PAYMENT_SUCCESS");
        event.setTransactionId("TXN-123");

        notificationService.handlePaymentCompleted(event);

        return ResponseEntity.ok("Test payment notification created");
    }
}