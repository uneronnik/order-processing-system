package com.example.inventory.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PostUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId(UUID id) {return id;}

    public String getProductId() {return productId;}

    public void setProductId(String productId) {this.productId = productId;}

    public String getName() {return name;}

    public void setName(String name) {this.name = name;}

    public Integer getTotalQuantity() {return totalQuantity;}

    public void setTotalQuantity(Integer totalQuantity) {this.totalQuantity = totalQuantity;}

    public Integer getReservedQuantity() {return reservedQuantity;}

    public void setReservedQuantity(Integer reservedQuantity) {this.reservedQuantity = reservedQuantity;}

    public BigDecimal getPrice() {return price;}

    public void setPrice(BigDecimal price) {this.price = price;}

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void Reserve(int quantity) {
        if(getReservedQuantity() + quantity <= getTotalQuantity()) {
            setReservedQuantity(getReservedQuantity() + quantity);
        }
    }

    public void ConfirmReservation(int quantity) {
        setTotalQuantity(getTotalQuantity() + quantity);
    }

    public void CancelReservation(int quantity) {
        setReservedQuantity(getReservedQuantity() - quantity);
    }
}