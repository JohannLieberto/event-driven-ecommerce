package com.ecommerce.orderservice.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderRequestTest {

    @Test
    void orderRequest_gettersAndSetters_workCorrectly() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(42L);
        request.setItems(List.of(itemRequest));

        assertEquals(42L, request.getCustomerId());
        assertEquals(1, request.getItems().size());
        assertEquals(1L, request.getItems().get(0).getProductId());
        assertEquals(3, request.getItems().get(0).getQuantity());
    }
}
