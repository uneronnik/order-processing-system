package com.example.payment.service;

import com.example.common.event.OrderCreatedEvent;
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

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}", event.orderId());
        paymentService.processPayment(event);
    }
}