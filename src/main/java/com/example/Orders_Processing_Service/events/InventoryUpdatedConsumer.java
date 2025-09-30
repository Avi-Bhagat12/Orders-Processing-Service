package com.example.Orders_Processing_Service.events;

import com.example.Orders_Processing_Service.entity.OrderEntity;
import com.example.Orders_Processing_Service.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Listens to the "inventory.updated" topic.
 * When stock levels change, updates the corresponding OrderEntity.
 */
@Component
public class InventoryUpdatedConsumer {

    private final OrderRepository orderRepository;

    public InventoryUpdatedConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Receives messages like:
     * {
     *   "orderId": "ORD123",
     *   "status": "OUT_OF_STOCK"   // or "RESTOCKED"
     * }
     */
    @KafkaListener(topics = "${topics.inventory-updated}", groupId = "orders-service-group")
    @Transactional
    public void onInventoryUpdated(@Payload InventoryUpdatedEvent event,
                                   ConsumerRecord<String, InventoryUpdatedEvent> record) {

        Optional<OrderEntity> optionalOrder = orderRepository.findByOrderId(event.getOrderId());
        if (optionalOrder.isEmpty()) {
            return; // No matching order found
        }

        OrderEntity order = optionalOrder.get();

        // Adjust order status based on inventory status
        switch (event.getStatus().toUpperCase()) {
            case "OUT_OF_STOCK" -> order.setStatus("ON_HOLD");
            case "RESTOCKED"    -> order.setStatus("READY");
            default             -> order.setStatus("PENDING");
        }

        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    /** DTO representing the expected JSON payload from the inventory service */
    public static class InventoryUpdatedEvent {
        private String orderId;
        private String status;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
