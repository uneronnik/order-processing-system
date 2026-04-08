package com.example.notification.consumer;

import com.example.notification.router.NotificationRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @Mock
    private NotificationRouter router;

    @InjectMocks
    private NotificationKafkaConsumer consumer;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    // ===================== payment-events =====================

    @Test
    void handlePaymentEvent_completed_notifiesWithProductAndAmount() {
        String json = """
                {
                    "paymentId": "123",
                    "orderId": "456",
                    "productId": "PHONE-001",
                    "quantity": 2,
                    "amount": 100000
                }
                """;

        consumer.handlePaymentEvent(json);

        verify(router).notifyAll(messageCaptor.capture());
        String msg = messageCaptor.getValue();
        assertTrue(msg.contains("PHONE-001"));
        assertTrue(msg.contains("2"));
        assertTrue(msg.contains("100000"));
    }

    @Test
    void handlePaymentEvent_failed_notifiesWithReason() {
        String json = """
                {
                    "orderId": "456",
                    "productId": "CAR-001",
                    "quantity": 1,
                    "reason": "Insufficient funds"
                }
                """;

        consumer.handlePaymentEvent(json);

        verify(router).notifyAll(messageCaptor.capture());
        String msg = messageCaptor.getValue();
        assertTrue(msg.contains("CAR-001"));
        assertTrue(msg.contains("Insufficient funds"));
    }

    @Test
    void handlePaymentEvent_invalidJson_doesNotThrow() {
        consumer.handlePaymentEvent("not valid json");

        verify(router, never()).notifyAll(any());
    }

    // ===================== inventory-events =====================

    @Test
    void handleInventoryEvent_reserved_notifiesWithProductAndAmount() {
        String json = """
                {
                    "orderId": "456",
                    "productId": "PHONE-001",
                    "quantity": 3,
                    "amount": 150000
                }
                """;

        consumer.handleInventoryEvent(json);

        verify(router).notifyAll(messageCaptor.capture());
        String msg = messageCaptor.getValue();
        assertTrue(msg.contains("PHONE-001"));
        assertTrue(msg.contains("3"));
        assertTrue(msg.contains("150000"));
    }

    @Test
    void handleInventoryEvent_reservationFailed_notifiesWithReason() {
        String json = """
                {
                    "orderId": "456",
                    "productId": "LAPTOP-001",
                    "reason": "Insufficient stock"
                }
                """;

        consumer.handleInventoryEvent(json);

        verify(router).notifyAll(messageCaptor.capture());
        String msg = messageCaptor.getValue();
        assertTrue(msg.contains("LAPTOP-001"));
        assertTrue(msg.contains("Insufficient stock"));
    }

    @Test
    void handleInventoryEvent_invalidJson_doesNotThrow() {
        consumer.handleInventoryEvent("broken json {{{");

        verify(router, never()).notifyAll(any());
    }
}