package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_successfullyPublishesKafkaEvent() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(100L);
        request.setItems(List.of(itemReq));

        when(inventoryClient.checkStock(1L, 2)).thenReturn(true);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId(100L);
        savedOrder.setStatus("PENDING");
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        OrderItem savedItem = new OrderItem();
        savedItem.setId(1L);
        savedItem.setProductId(1L);
        savedItem.setQuantity(2);
        savedOrder.setItems(List.of(savedItem));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(orderEventPublisher).publishOrderCreated(any());

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(orderEventPublisher, times(1)).publishOrderCreated(any());
        verify(inventoryClient, never()).reserveStock(anyLong(), anyInt(), anyLong());
    }

    @Test
    void createOrder_insufficientStock_throwsException() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(2L);
        itemReq.setQuantity(999);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(100L);
        request.setItems(List.of(itemReq));

        when(inventoryClient.checkStock(2L, 999)).thenReturn(false);

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));
        verify(orderEventPublisher, never()).publishOrderCreated(any());
    }

    @Test
    void getOrderById_returnsOrder() {
        Order order = new Order();
        order.setId(5L);
        order.setCustomerId(10L);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(List.of());

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(5L);
        assertEquals(5L, response.getId());
        assertEquals("PENDING", response.getStatus());
    }
}
