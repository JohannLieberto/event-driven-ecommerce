package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_Success() {

        // Arrange
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1001L);

        List<OrderItemRequest> items = new ArrayList<>();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(101L);
        item.setQuantity(2);
        items.add(item);
        request.setItems(items);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId(1001L);
        savedOrder.setStatus("CONFIRMED");

        when(inventoryClient.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1001L, response.getCustomerId());
        assertEquals("CONFIRMED", response.getStatus());

       // verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void getOrderById_Found() {

        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1001L);
        order.setStatus("CONFIRMED");
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1001L, response.getCustomerId());
    }

    @Test
    void getOrderById_NotFound() {

        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(999L);
        });
    }
}