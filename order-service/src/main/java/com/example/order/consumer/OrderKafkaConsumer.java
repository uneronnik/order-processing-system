package com.example.order.consumer;

import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderKafkaConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());;
    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    public OrderKafkaConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentResult(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.has("paymentId")) {
                PaymentCompletedEvent event = objectMapper.treeToValue(json, PaymentCompletedEvent.class);
                handlePaymentCompleted(event);
            } else if (json.has("reason")) {
                PaymentFailedEvent event = objectMapper.treeToValue(json, PaymentFailedEvent.class);
                handlePaymentFailed(event);
            } else {
                log.warn("Unknown event type in payment-events: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} confirmed after payment", event.orderId());
        });
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.warn("Order {} cancelled: {}", event.orderId(), event.reason());
        });
    }
}