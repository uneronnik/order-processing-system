package com.example.payment.service;

import com.example.common.event.InventoryReservedEvent;
import com.example.common.event.OrderCreatedEvent;
import com.example.payment.producer.PaymentKafkaProducer;
import com.example.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock private PaymentKafkaProducer kafkaProducer;
    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_shouldCompleteSuccessfully() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), "PHONE-001", 1, new BigDecimal("50000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(event.orderId())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(paymentRepository).save(any());
        verify(kafkaProducer).sendPaymentCompleted(any());
    }

    @Test
    void processPayment_whenAlreadyProcessed_shouldSkip() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), "PHONE-001", 1, new BigDecimal("50000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(event.orderId())).thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verify(kafkaProducer, never()).sendPaymentCompleted(any());
    }

    @Test
    void processPayment_withHighAmount_shouldFail() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(), "CAR-001", 1, new BigDecimal("150000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(event.orderId())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(kafkaProducer).sendPaymentFailed(any());
    }
}