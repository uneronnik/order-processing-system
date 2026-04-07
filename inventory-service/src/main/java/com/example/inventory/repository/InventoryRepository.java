package com.example.inventory.repository;

import com.example.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {
    InventoryItem findByProductId(String productId);
}
