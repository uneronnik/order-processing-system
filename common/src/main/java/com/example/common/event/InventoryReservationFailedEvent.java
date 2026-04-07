package com.example.common.event;

import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID orderId,
        String productId,
        String reason
) {
}
