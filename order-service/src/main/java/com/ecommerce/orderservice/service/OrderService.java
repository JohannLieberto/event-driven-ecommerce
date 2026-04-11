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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    // =============================
    // CREATE ORDER
    // =============================
    public OrderResponse createOrder(OrderRequest request) {

        log.info("🟡 Creating order for customerId={}", request.getCustomerId());

        // STEP 1: Check stock
        for (OrderItemRequest itemRequest : request.getItems()) {
            boolean hasStock = inventoryClient.checkStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            if (!hasStock) {
                log.error("❌ Insufficient stock for productId={}", itemRequest.getProductId());
                throw new InsufficientStockException(
                        "Insufficient stock for product " + itemRequest.getProductId()
                );
            }
        }

        // STEP 2: Create order
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

        log.info("✅ Order saved with ID={}", savedOrder.getId());

        // STEP 3: Convert to event items
        List<OrderItemEvent> itemEvents = savedOrder.getItems().stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());

        // STEP 4: Create event
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getStatus(),
                itemEvents,
                savedOrder.getCreatedAt()
        );

        // STEP 5: Publish event 🚀
        log.info("🚀 Publishing order.created event for orderId={}", savedOrder.getId());

        try {
            orderEventPublisher.publishOrderCreated(event);
            log.info("✅ Event published successfully for orderId={}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish Kafka event for orderId={}", savedOrder.getId(), e);
            throw e;
        }

        return mapToResponse(savedOrder);
    }

    // =============================
    // GET ORDER BY ID
    // =============================
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with ID: " + id));

        return mapToResponse(order);
    }

    // =============================
    // GET ORDERS BY CUSTOMER
    // =============================
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);

        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // =============================
    // UPDATE STATUS
    // =============================
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with ID: " + id));

        order.setStatus(status);
        Order updated = orderRepository.save(order);

        return mapToResponse(updated);
    }


    // =============================
    // MAPPER
    // =============================
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