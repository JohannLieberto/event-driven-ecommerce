package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_Success() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId("CUST001");

        List<OrderItemRequest> items = new ArrayList<>();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(101L);
        item.setQuantity(2);
        items.add(item);
        request.setItems(items);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId("CUST001");
        savedOrder.setStatus("PENDING");

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CUST001", response.getCustomerId());
        assertEquals("PENDING", response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrderById_Found() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId("CUST001");
        order.setStatus("PENDING");
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CUST001", response.getCustomerId());
    }

    @Test
    void getOrderById_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(999L);
        });
    }
}
