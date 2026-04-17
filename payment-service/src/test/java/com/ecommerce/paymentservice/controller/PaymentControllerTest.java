package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.config.SecurityConfig;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.kafka.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void processPayment_newPayment_returns200WithTransactionId() throws Exception {
        Payment saved = payment(1L, 1L, 100L, "PAYMENT_SUCCESS", "txn-001");
        when(paymentRepository.findFirstByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("orderId", 1, "customerId", 100, "amount", 49.99))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PAYMENT_SUCCESS"));

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentEventPublisher).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_existingPayment_republishesAndReturns200() throws Exception {
        Payment existing = payment(1L, 1L, 100L, "PAYMENT_SUCCESS", "txn-existing");
        when(paymentRepository.findFirstByOrderId(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("orderId", 1, "customerId", 100, "amount", 49.99))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-existing"));

        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("customerId", 100, "amount", 49.99))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("orderId", 1, "customerId", 100))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentByOrderId_exists_returns200() throws Exception {
        Payment p = payment(1L, 42L, 200L, "PAYMENT_SUCCESS", "txn-42");
        when(paymentRepository.findFirstByOrderId(42L)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/payments/order/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42));
    }

    @Test
    void getPaymentByOrderId_notFound_returns404() throws Exception {
        when(paymentRepository.findFirstByOrderId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/order/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("payment-service is running"));
    }

    private Payment payment(Long id, Long orderId, Long customerId, String status, String txnId) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrderId(orderId);
        p.setCustomerId(customerId);
        p.setAmount(BigDecimal.valueOf(49.99));
        p.setStatus(status);
        p.setTransactionId(txnId);
        return p;
    }
}
