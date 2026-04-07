package com.example.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateInventoryItemRequest(
        @NotBlank(message = "productId is required")
        String productId,

        @NotBlank(message = "name is required")
        String name,

        @NotNull @Min(0)
        Integer totalQuantity,

        @NotNull @DecimalMin("0.01")
        BigDecimal price
) {
}
