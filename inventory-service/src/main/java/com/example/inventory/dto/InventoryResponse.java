package com.example.inventory.dto;

import com.example.inventory.entity.InventoryItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        String productId,
        String name,
        Integer totalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        BigDecimal price,
        LocalDateTime updatedAt
) {
    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(
                item.getId(),
                item.getProductId(),
                item.getName(),
                item.getTotalQuantity(),
                item.getReservedQuantity(),
                item.getTotalQuantity() - item.getReservedQuantity(),
                item.getPrice(),
                item.getUpdatedAt()
        );
    }
}