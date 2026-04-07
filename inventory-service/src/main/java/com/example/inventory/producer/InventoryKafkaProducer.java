package com.example.inventory.producer;

import com.example.common.event.InventoryReservationFailedEvent;
import com.example.common.event.InventoryReservedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "inventory-events";
    public InventoryKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendReservationSuccess(InventoryReservedEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    }
    public void sendReservationFailed(InventoryReservationFailedEvent event) {
        kafkaTemplate.send(TOPIC, event.id().toString(), event);
    }
}
