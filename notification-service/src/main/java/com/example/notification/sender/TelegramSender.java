package com.example.notification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "telegram.bot-token", havingValue = "", matchIfMissing = false)
public class TelegramSender implements NotificationSender {

    private final RestClient restClient;
    @Value("${telegram.bot-token}") private String botToken;
    @Value("${telegram.chat-id}") private String chatId;

    public TelegramSender(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void send(String message) {
        restClient.post()
                .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                .body(Map.of("chat_id", chatId, "text", message))
                .retrieve()
                .toBodilessEntity();
    }

    public String getType() { return "telegram"; }
}