package com.example.Orders_Processing_Service.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

    @Component
    public class OrderEventPublisher {

        private final KafkaTemplate<String, OrderEvents> kafkaTemplate;

        @Value("${topics.order-created}")
        private String orderCreatedTopic;

        @Value("${topics.order-failed}")
        private String orderFailedTopic;

        public OrderEventPublisher(KafkaTemplate<String, OrderEvents> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void publishOrderCreated(OrderEvents event) {
            kafkaTemplate.send(orderCreatedTopic, event);
        }

        public void publishOrderFailed(OrderEvents event) {
            kafkaTemplate.send(orderFailedTopic, event);
        }
    }

