package com.example.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID orderId,
        String reason,
        LocalDateTime failedAt
) {
}
