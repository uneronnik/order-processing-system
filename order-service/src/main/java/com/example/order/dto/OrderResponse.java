package com.example.order.dto;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String productId,
        Integer quantity,
        BigDecimal amount,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}