package com.example.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String productId,
        Integer quantity,
        BigDecimal amount,
        LocalDateTime createdAt
) {}