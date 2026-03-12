package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    public OrderResponse createOrder(OrderRequest request) {
        // STEP 1: Validate stock for all items BEFORE creating order
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

        // STEP 2: Create order entity
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus("PENDING");

        // Map order items
        List<OrderItem> items = request.getItems().stream()
            .map(itemRequest -> {
                OrderItem item = new OrderItem();
                item.setProductId(itemRequest.getProductId());
                item.setQuantity(itemRequest.getQuantity());
                return item;
            })
            .toList();

        order.setItems(items);

        // STEP 3: Save order to database
        Order savedOrder = orderRepository.save(order);

        // STEP 4: Reserve stock in inventory service
        for (OrderItemRequest itemRequest : request.getItems()) {
            inventoryClient.reserveStock(
                itemRequest.getProductId(),
                itemRequest.getQuantity(),
                savedOrder.getId()
            );
        }

        // STEP 5: Update order status to CONFIRMED
        savedOrder.setStatus("CONFIRMED");
        Order confirmedOrder = orderRepository.save(savedOrder);

        return mapToResponse(confirmedOrder);
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
            .toList();
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
            .toList();

        response.setItems(itemResponses);
        return response;
    }
}
