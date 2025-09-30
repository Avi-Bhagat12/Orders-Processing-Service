package com.example.Orders_Processing_Service.Service;

import com.example.Orders_Processing_Service.entity.OrderEntity;
import com.example.Orders_Processing_Service.events.OrderEvents;
import com.example.Orders_Processing_Service.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class EventService {

    private final KafkaTemplate<String, OrderEvents> kafkaTemplate;
    private final OrderRepository orderRepository;

    @Value("${topics.order-created}")
    private String orderCreatedTopic;

    @Value("${topics.order-failed}")
    private String orderFailedTopic;

    public EventService(KafkaTemplate<String, OrderEvents> kafkaTemplate,
                                 OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    /** Publish an "order.created" event */
    public void publishOrderCreated(OrderEntity order) {
        OrderEvents event = new OrderEvents();
        event.setStatus("CREATED");
        event.setOrderId(order.getOrderId());
        event.setMessage("Order successfully created at " + Instant.now());
        kafkaTemplate.send(orderCreatedTopic, event);
    }

    /** Publish an "order.failed" event */
    public void publishOrderFailed(String orderId, String reason) {
        OrderEvents event = new OrderEvents();
        event.setStatus("FAILED");
        event.setOrderId(orderId);
        event.setMessage(reason);
        kafkaTemplate.send(orderFailedTopic, event);
    }

    /**
     * Consumer for "inventory.updated" topic.
     * Expects a JSON payload such as:
     * {
     *   "orderId": "ORD123",
     *   "status": "OUT_OF_STOCK"   // or "RESTOCKED"
     * }
     */
    @KafkaListener(topics = "${topics.inventory-updated}", groupId = "orders-service-group")
    @Transactional
    public void onInventoryUpdated(@Payload InventoryUpdatedEvent event,
                                   ConsumerRecord<String, InventoryUpdatedEvent> record) {
        Optional<OrderEntity> orderOpt = orderRepository.findByOrderId(event.getOrderId());
        if (orderOpt.isEmpty()) {
            return;
        }
        OrderEntity order = orderOpt.get();

        if ("OUT_OF_STOCK".equalsIgnoreCase(event.getStatus())) {
            order.setStatus("ON_HOLD");
        } else if ("RESTOCKED".equalsIgnoreCase(event.getStatus())) {
            order.setStatus("READY");
        }

        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    /** Inner DTO matching the incoming inventory.updated payload */
    public static class InventoryUpdatedEvent {
        private String orderId;
        private String status; // OUT_OF_STOCK, RESTOCKED, etc.

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
