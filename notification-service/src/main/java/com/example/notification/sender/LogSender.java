package com.example.notification.sender;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LogSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LogSender.class);

    public void send(String message) {
        log.info("NOTIFICATION: {}", message);
    }

    public String getType() { return "log"; }
}