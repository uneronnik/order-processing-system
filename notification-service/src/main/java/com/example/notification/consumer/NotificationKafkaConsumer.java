package com.example.notification.consumer;

import com.example.notification.router.NotificationRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationKafkaConsumer {

    private final NotificationRouter router;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    public NotificationKafkaConsumer(NotificationRouter router) {
        this.router = router;
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.has("paymentId")) {
                router.notifyAll(String.format(
                        "Payment successful! Product: %s, quantity: %s, total: %s",
                        json.get("productId").asText(),
                        json.get("quantity").asText(),
                        json.get("amount").asText()
                ));
            } else if (json.has("reason")) {
                router.notifyAll(String.format(
                        "Payment failed for %s (x%s): %s",
                        json.get("productId").asText(),
                        json.get("quantity").asText(),
                        json.get("reason").asText()
                ));
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "notification-service")
    public void handleInventoryEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.has("reason")) {
                router.notifyAll(String.format(
                        "Order cancelled — %s not available: %s",
                        json.get("productId").asText(),
                        json.get("reason").asText()
                ));
            } else if (json.has("amount")) {
                router.notifyAll(String.format(
                        "Product reserved: %s (x%s), total: %s. Proceeding to payment.",
                        json.get("productId").asText(),
                        json.get("quantity").asText(),
                        json.get("amount").asText()
                ));
            }
        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", e.getMessage());
        }
    }
}