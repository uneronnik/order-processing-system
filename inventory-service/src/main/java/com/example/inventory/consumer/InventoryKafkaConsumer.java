package com.example.inventory.consumer;

import com.example.common.event.InventoryReservationFailedEvent;
import com.example.common.event.OrderCreatedEvent;
import com.example.inventory.exceptions.InsufficientStockException;
import com.example.inventory.exceptions.ProductNotFoundException;
import com.example.inventory.producer.InventoryKafkaProducer;
import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryKafkaConsumer {
    private final InventoryService inventoryService;
    private final InventoryKafkaProducer kafkaProducer;

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaProducer.class);

    public InventoryKafkaConsumer(InventoryService inventoryService, InventoryKafkaProducer kafkaProducer) {
        this.inventoryService = inventoryService;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}", event.orderId());
        try {
            inventoryService.reserveItem(event);
        } catch (ProductNotFoundException e) {
            kafkaProducer.sendReservationFailed(new InventoryReservationFailedEvent(
                    event.orderId(), event.productId(), "Product not found"
            ));
        } catch (InsufficientStockException e) {
            kafkaProducer.sendReservationFailed(new InventoryReservationFailedEvent(
                    event.orderId(), event.productId(), "Insufficient stock"
            ));
        }
    }
}
