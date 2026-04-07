package com.example.payment.consumer;

import com.example.common.event.InventoryReservedEvent;
import com.example.payment.service.PaymentService;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;


@Service
public class PaymentKafkaConsumer {

    private final PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaConsumer.class);

    public PaymentKafkaConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "inventory-events", groupId = "payment-service")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent: orderId={}", event.orderId());
        paymentService.processPayment(event);
    }
}