package com.example.Orders_Processing_Service.Service;

import com.example.Orders_Processing_Service.entity.OrderEntity;
import com.example.Orders_Processing_Service.model.request.orderRequest;
import com.example.Orders_Processing_Service.model.response.orderResponse;
import com.example.Orders_Processing_Service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PostJson {

    private final OrderRepository orderRepository;
    private final EventService eventService;   // ✅ Injected here

    // Constructor injection
    public PostJson(OrderRepository orderRepository,
                    EventService eventService) {
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }

    @Transactional
    public orderResponse createOrder(orderRequest request) {
        OrderEntity entity = new OrderEntity();

        entity.setOrderId(request.getOrderId());
        entity.setCustomerId(request.getCustomerId());
        entity.setItemJson("[]"); // Placeholder for now
        entity.setTotal(new BigDecimal(request.getOrderAmount()));
        entity.setStatus(request.getOrderStatus());

        try {
            entity.setCreatedAt(Instant.parse(request.getOrderDate()));
        } catch (Exception e) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());

        // Save to DB
        orderRepository.save(entity);

        // ✅ Publish the "order.created" event after successful save
        eventService.publishOrderCreated(entity);

        // Build the response
        orderResponse response = new orderResponse();
        response.setOrderId(entity.getOrderId());
        response.setOrderStatus(entity.getStatus());
        response.setOrderAmount(entity.getTotal().toString());
        response.setOrderDate(entity.getCreatedAt().toString());
        response.setCustomerId(entity.getCustomerId());

        return response;
    }

    /**
     * Optional helper for failed orders.
     * Call this if you catch a business error and want to publish an "order.failed" event.
     */
    public void publishOrderFailure(String orderId, String reason) {
        eventService.publishOrderFailed(orderId, reason);
    }
}


