package com.example.common.event;

import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID id,
        String productId,
        String reason
) {
}
