package com.example.notification.router;

import com.example.notification.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationRouter {

    private final List<NotificationSender> senders;
    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    public NotificationRouter(List<NotificationSender> senders) {
        this.senders = senders;
        log.info("Active notification channels: {}",
                senders.stream().map(NotificationSender::getType).toList());
    }

    public void notifyAll(String message) {
        senders.forEach(s -> {
            try {
                s.send(message);
            } catch (Exception e) {
                log.error("Failed to send via {}: {}", s.getType(), e.getMessage());
            }
        });
    }
}