package com.ecommerce.shippingservice.controller;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentRepository shipmentRepository;

    @Test
    void getShipmentByOrderId_exists_returns200() throws Exception {
        Shipment s = shipment(1L, 10L, 100L, "SHIPMENT_SCHEDULED", "TRK-001");
        when(shipmentRepository.findFirstByOrderId(10L)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/shipments/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.trackingNumber").value("TRK-001"));
    }

    @Test
    void getShipmentByOrderId_notFound_returns404() throws Exception {
        when(shipmentRepository.findFirstByOrderId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/shipments/order/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getShipmentsByCustomerId_returnsList() throws Exception {
        Shipment s1 = shipment(1L, 10L, 200L, "SHIPMENT_SCHEDULED", "TRK-A");
        Shipment s2 = shipment(2L, 11L, 200L, "SHIPMENT_SCHEDULED", "TRK-B");
        when(shipmentRepository.findByCustomerId(200L)).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/shipments/customer/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getShipmentsByCustomerId_noShipments_returnsEmptyList() throws Exception {
        when(shipmentRepository.findByCustomerId(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/shipments/customer/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/shipments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("shipping-service is running"));
    }

    private Shipment shipment(Long id, Long orderId, Long customerId, String status, String tracking) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setOrderId(orderId);
        s.setCustomerId(customerId);
        s.setStatus(status);
        s.setTrackingNumber(tracking);
        return s;
    }
}
