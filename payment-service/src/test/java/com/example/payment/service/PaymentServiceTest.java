package com.example.payment.service;

import com.example.common.event.InventoryReservedEvent;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.producer.PaymentKafkaProducer;
import com.example.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentKafkaProducer kafkaProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @Captor
    private ArgumentCaptor<PaymentCompletedEvent> completedEventCaptor;

    @Captor
    private ArgumentCaptor<PaymentFailedEvent> failedEventCaptor;

    @Test
    void processPayment_success_savesPaymentWithCorrectFields() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "PHONE-001", 2, new BigDecimal("50000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertEquals(new BigDecimal("50000"), saved.getAmount());
        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
    }

    @Test
    void processPayment_success_sendsCompletedEvent() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "PHONE-001", 2, new BigDecimal("50000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(kafkaProducer).sendPaymentCompleted(completedEventCaptor.capture());
        verify(kafkaProducer, never()).sendPaymentFailed(any());

        PaymentCompletedEvent sent = completedEventCaptor.getValue();
        assertEquals(orderId, sent.orderId());
        assertEquals("PHONE-001", sent.productId());
        assertEquals(2, sent.quantity());
        assertEquals(new BigDecimal("50000"), sent.amount());
    }

    @Test
    void processPayment_highAmount_savesPaymentAsFailed() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "CAR-001", 1, new BigDecimal("150000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());
    }

    @Test
    void processPayment_highAmount_sendsFailedEvent() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "CAR-001", 1, new BigDecimal("150000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(kafkaProducer).sendPaymentFailed(failedEventCaptor.capture());
        verify(kafkaProducer, never()).sendPaymentCompleted(any());

        PaymentFailedEvent sent = failedEventCaptor.getValue();
        assertEquals(orderId, sent.orderId());
        assertEquals("CAR-001", sent.productId());
        assertEquals(1, sent.quantity());
    }

    @Test
    void processPayment_exactBoundary_shouldFail() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "ITEM-001", 1, new BigDecimal("100000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(event);

        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());
        verify(kafkaProducer).sendPaymentFailed(any());
        verify(kafkaProducer, never()).sendPaymentCompleted(any());
    }

    @Test
    void processPayment_alreadyProcessed_skipsEntirely() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, "PHONE-001", 1, new BigDecimal("50000"), LocalDateTime.now());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verify(kafkaProducer, never()).sendPaymentCompleted(any());
        verify(kafkaProducer, never()).sendPaymentFailed(any());
    }
}