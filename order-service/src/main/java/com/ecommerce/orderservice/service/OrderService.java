package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    public OrderResponse createOrder(OrderRequest request) {
        // STEP 1: Synchronous stock check (read-only, keeps fast feedback)
        for (OrderItemRequest itemRequest : request.getItems()) {
            boolean hasStock = inventoryClient.checkStock(
                itemRequest.getProductId(),
                itemRequest.getQuantity()
            );
            if (!hasStock) {
                throw new InsufficientStockException(
                    "Insufficient stock for product " + itemRequest.getProductId()
                );
            }
        }

        // STEP 2: Create and persist order as PENDING
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus("PENDING");

        List<OrderItem> items = request.getItems().stream()
            .map(itemRequest -> {
                OrderItem item = new OrderItem();
                item.setProductId(itemRequest.getProductId());
                item.setQuantity(itemRequest.getQuantity());
                return item;
            })
            .collect(Collectors.toCollection(ArrayList::new));

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        // STEP 3: Publish order.created event — downstream services handle the rest asynchronously
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = savedOrder.getItems().stream()
            .map(item -> new OrderCreatedEvent.OrderItemEvent(item.getProductId(), item.getQuantity()))
            .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
            savedOrder.getId(),
            savedOrder.getCustomerId(),
            savedOrder.getStatus(),
            itemEvents,
            savedOrder.getCreatedAt()
        );

        orderEventPublisher.publishOrderCreated(event);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(item -> {
                OrderItemResponse itemResponse = new OrderItemResponse();
                itemResponse.setId(item.getId());
                itemResponse.setProductId(item.getProductId());
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setPrice(item.getPrice());
                return itemResponse;
            })
            .collect(Collectors.toCollection(ArrayList::new));

        response.setItems(itemResponses);
        return response;
    }
}
