package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.event.InventoryReservedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String msg = buildPaymentMessage(event);
        saveAndLog(event.getOrderId(), event.getCustomerId(), event.getStatus(), msg);
    }

    public void handleShipmentScheduled(ShipmentScheduledEvent event) {
        String msg = String.format(
            "Your order #%d has been shipped! Tracking: %s",
            event.getOrderId(), event.getTrackingNumber()
        );
        saveAndLog(event.getOrderId(), event.getCustomerId(), event.getStatus(), msg);
    }

    public void handleInventoryReserved(InventoryReservedEvent event) {
        String msg = String.format(
            "Inventory status for order #%d: %s — %s",
            event.getOrderId(), event.getStatus(), event.getMessage()
        );
        saveAndLog(event.getOrderId(), event.getCustomerId(), event.getStatus(), msg);
    }

    private void saveAndLog(Long orderId, Long customerId, String eventType, String message) {
        Notification notification = new Notification();
        notification.setOrderId(orderId);
        notification.setCustomerId(customerId);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setChannel("EMAIL");
        notificationRepository.save(notification);
        log.info("[NOTIFICATION-SERVICE] Sent notification orderId={} customerId={} type={} msg='{}'",
            orderId, customerId, eventType, message);
    }

    private String buildPaymentMessage(PaymentCompletedEvent event) {
        if ("PAYMENT_SUCCESS".equals(event.getStatus())) {
            return String.format(
                "Payment successful for order #%d. Transaction ID: %s",
                event.getOrderId(), event.getTransactionId()
            );
        } else {
            return String.format(
                "Payment failed for order #%d. Please retry or contact support.",
                event.getOrderId()
            );
        }
    }
}
