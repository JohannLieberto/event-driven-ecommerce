package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.event.InventoryReservedEvent;
import com.ecommerce.notificationservice.event.PaymentCompletedEvent;
import com.ecommerce.notificationservice.event.ShipmentScheduledEvent;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailSenderService emailSenderService;

    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String msg = buildPaymentMessage(event);
        saveAndLog(
                event.getOrderId(),
                event.getCustomerId(),
                event.getEmail(),
                NotificationType.ORDER_CONFIRMED,
                event.getStatus(),
                msg
        );
    }

    public void handleShipmentScheduled(ShipmentScheduledEvent event) {
        String msg = String.format(
                "Your order #%d has been shipped! Tracking: %s",
                event.getOrderId(), event.getTrackingNumber()
        );
        saveAndLog(
                event.getOrderId(),
                event.getCustomerId(),
                event.getEmail(),
                NotificationType.SHIPPED,
                event.getStatus(),
                msg
        );
    }

    public void handleInventoryReserved(InventoryReservedEvent event) {
        String msg = String.format(
                "Inventory status for order #%d: %s — %s",
                event.getOrderId(), event.getStatus(), event.getMessage()
        );
        saveAndLog(
                event.getOrderId(),
                event.getCustomerId(),
                event.getEmail(),
                NotificationType.ORDER_CONFIRMED,
                event.getStatus(),
                msg
        );
    }

    private void saveAndLog(Long orderId,
                            Long customerId,
                            String email,
                            NotificationType type,
                            String eventType,
                            String message) {

        Notification notification = new Notification();
        notification.setOrderId(orderId);
        notification.setCustomerId(customerId);
        notification.setEmail(email);
        notification.setType(type);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setChannel("EMAIL");

        try {
            emailSenderService.sendEmail(email, type.name(), message);
            notification.setStatus(NotificationStatus.SENT);
            notification.setFailureReason(null);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
            log.error("[NOTIFICATION-SERVICE] Failed to send email orderId={} email={} reason={}",
                    orderId, email, e.getMessage());
        }

        notificationRepository.save(notification);

        log.info("[NOTIFICATION-SERVICE] Sent notification orderId={} customerId={} email={} type={} msg='{}'",
                orderId, customerId, email, eventType, message);
    }

    private String buildPaymentMessage(PaymentCompletedEvent event) {
        if ("PAYMENT_SUCCESS".equalsIgnoreCase(event.getStatus())
                || "SUCCESS".equalsIgnoreCase(event.getStatus())
                || "COMPLETED".equalsIgnoreCase(event.getStatus())) {
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

    public List<Notification> getNotificationsByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId);
    }

    public List<Notification> getNotificationsByCustomerId(Long customerId) {
        return notificationRepository.findByCustomerId(customerId);
    }
}