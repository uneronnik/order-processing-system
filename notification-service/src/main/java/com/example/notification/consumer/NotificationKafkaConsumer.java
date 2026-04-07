package com.example.notification.consumer;

import com.example.common.event.PaymentCompletedEvent;
import com.example.notification.router.NotificationRouter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationKafkaConsumer {

    private final NotificationRouter router;

    public NotificationKafkaConsumer(NotificationRouter router) {
        this.router = router;
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        router.notifyAll(String.format(
                "Заказ %s подтверждён. Сумма: %s ₽",
                event.orderId(), event.amount()
        ));
    }
}