package com.example.common.event;

import java.util.UUID;

public record InventoryReservedEvent(
        UUID id,
        String productId,
        Integer quantity

) {
}
