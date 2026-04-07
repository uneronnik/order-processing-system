package com.example.notification.sender;

public interface NotificationSender {
    void send(String message);
    String getType();
}
