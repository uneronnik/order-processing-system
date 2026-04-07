package com.example.payment.service;

import com.example.common.event.InventoryReservedEvent;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.producer.PaymentKafkaProducer;
import com.example.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer kafkaProducer;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(PaymentRepository paymentRepository, PaymentKafkaProducer kafkaProducer) {
        this.paymentRepository = paymentRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public void processPayment(InventoryReservedEvent event) {
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("Payment already processed for orderId={}, skipping", event.orderId());
            return;
        }

        boolean success = simulatePayment(event.amount());

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(event.amount());
        payment.setStatus(success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        paymentRepository.save(payment);

        if (success) {
            kafkaProducer.sendPaymentCompleted(new PaymentCompletedEvent(
                    payment.getId(), event.orderId(), event.amount(), LocalDateTime.now()
            ));
            log.info("Payment completed for orderId={}", event.orderId());
        } else {
            kafkaProducer.sendPaymentFailed(new PaymentFailedEvent(
                    event.orderId(), "Insufficient funds", LocalDateTime.now()
            ));
            log.warn("Payment failed for orderId={}", event.orderId());
        }
    }

    private boolean simulatePayment(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("100000")) < 0;
    }
}