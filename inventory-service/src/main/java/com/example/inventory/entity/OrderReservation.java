package com.example.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_reservations")
public class OrderReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    public OrderReservation() {}

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public LocalDateTime getReservedAt() { return reservedAt; }

    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
}
