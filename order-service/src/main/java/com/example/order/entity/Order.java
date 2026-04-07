package com.example.order.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.annotation.processing.Generated;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = true)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public Order() {}

    public UUID getId() { return id; }
    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setProductId(String productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setId(UUID uuid) {this.id = uuid;}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
