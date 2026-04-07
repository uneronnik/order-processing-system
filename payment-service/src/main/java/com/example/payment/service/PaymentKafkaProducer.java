package com.example.payment.service;

import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaProducer.class);
    private static final String TOPIC = "payment-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
        log.info("Sent PaymentCompletedEvent for orderId={}", event.orderId());
    }

    public void sendPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
        log.info("Sent PaymentFailedEvent for orderId={}", event.orderId());
    }
}