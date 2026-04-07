package com.example.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID id,
        String productId,
        Integer quantity,
        BigDecimal amount,
        LocalDateTime reservedAt
) {
}
