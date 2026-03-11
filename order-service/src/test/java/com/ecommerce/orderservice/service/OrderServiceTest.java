package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private OrderRequest sampleRequest;

    @BeforeEach
    void setUp() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setQuantity(2);

        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setCustomerId(100L);
        sampleOrder.setStatus("CONFIRMED");
        sampleOrder.setCreatedAt(LocalDateTime.now());
        sampleOrder.setUpdatedAt(LocalDateTime.now());
        sampleOrder.setItems(new ArrayList<>(List.of(item)));

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        sampleRequest = new OrderRequest();
        sampleRequest.setCustomerId(100L);
        sampleRequest.setItems(List.of(itemRequest));
    }

    @Test
    void createOrder_sufficientStock_returnsConfirmedOrder() {
        when(inventoryClient.checkStock(10L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.createOrder(sampleRequest);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(100L, response.getCustomerId());
        verify(inventoryClient).checkStock(10L, 2);
        verify(inventoryClient).reserveStock(eq(10L), eq(2), anyLong());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_insufficientStock_throwsException() {
        when(inventoryClient.checkStock(10L, 2)).thenReturn(false);

        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(sampleRequest));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_found_returnsResponse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getCustomerId());
        assertEquals("CONFIRMED", response.getStatus());
        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void getOrdersByCustomerId_returnsList() {
        when(orderRepository.findByCustomerId(100L)).thenReturn(List.of(sampleOrder));

        List<OrderResponse> responses = orderService.getOrdersByCustomerId(100L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).getCustomerId());
    }

    @Test
    void getOrdersByCustomerId_noOrders_returnsEmptyList() {
        when(orderRepository.findByCustomerId(999L)).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getOrdersByCustomerId(999L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void createOrder_multipleItems_checksAllStock() {
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(10L);
        item1.setQuantity(1);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(20L);
        item2.setQuantity(3);

        OrderRequest multiItemRequest = new OrderRequest();
        multiItemRequest.setCustomerId(100L);
        multiItemRequest.setItems(List.of(item1, item2));

        when(inventoryClient.checkStock(10L, 1)).thenReturn(true);
        when(inventoryClient.checkStock(20L, 3)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.createOrder(multiItemRequest);
        assertNotNull(response);
        verify(inventoryClient).checkStock(10L, 1);
        verify(inventoryClient).checkStock(20L, 3);
    }
}
