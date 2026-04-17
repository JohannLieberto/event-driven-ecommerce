package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.EmailSenderService;
import com.ecommerce.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private EmailSenderService emailSenderService;

    @Test
    void getByCustomer_returnsList() throws Exception {
        Notification n = notification(1L, 10L, 100L, NotificationType.ORDER_CONFIRMED);
        when(notificationService.getNotificationsByCustomerId(100L)).thenReturn(List.of(n));

        mockMvc.perform(get("/api/notifications/customer/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(10));
    }

    @Test
    void getByCustomer_noNotifications_returnsEmptyList() throws Exception {
        when(notificationService.getNotificationsByCustomerId(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/customer/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByOrder_returnsList() throws Exception {
        Notification n = notification(1L, 42L, 100L, NotificationType.SHIPPED);
        when(notificationService.getNotificationsByOrderId(42L)).thenReturn(List.of(n));

        mockMvc.perform(get("/api/notifications/order/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(42));
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("notification-service is running"));
    }

    @Test
    void testEmail_triggersEmailSenderAndReturns200() throws Exception {
        doNothing().when(emailSenderService).sendEmail(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/notifications/test-email"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test email triggered successfully"));

        verify(emailSenderService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testPaymentFlow_triggersNotificationServiceAndReturns200() throws Exception {
        doNothing().when(notificationService).handlePaymentCompleted(any());

        mockMvc.perform(post("/api/notifications/test-payment"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test payment notification created"));

        verify(notificationService).handlePaymentCompleted(any());
    }

    private Notification notification(Long id, Long orderId, Long customerId, NotificationType type) {
        Notification n = new Notification();
        n.setId(id);
        n.setOrderId(orderId);
        n.setCustomerId(customerId);
        n.setType(type);
        n.setEventType("PAYMENT_SUCCESS");
        n.setMessage("Test message");
        n.setChannel("EMAIL");
        n.setStatus(NotificationStatus.SENT);
        return n;
    }
}
