package com.example.order.consumer;

import com.example.common.event.InventoryReservationFailedEvent;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
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

    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());;
    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    public OrderKafkaConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentResult(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.has("paymentId")) {
                PaymentCompletedEvent event = objectMapper.treeToValue(json, PaymentCompletedEvent.class);
                orderService.updateStatus(event.orderId(), OrderStatus.CONFIRMED, "Payment completed", event.amount());
            } else if (json.has("reason")) {
                PaymentFailedEvent event = objectMapper.treeToValue(json, PaymentFailedEvent.class);
                orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED, event.reason(), null);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service")
    public void handleInventoryResult(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.has("reason")) {
                InventoryReservationFailedEvent event = objectMapper.treeToValue(
                        json, InventoryReservationFailedEvent.class);
                orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED, event.reason(), null);
            }
        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", e.getMessage());
        }
    }
}