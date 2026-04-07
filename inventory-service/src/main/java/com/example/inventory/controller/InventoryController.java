package com.example.inventory.controller;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryResponse response = inventoryService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }
    @PutMapping("/{productId}/restock")
    public ResponseEntity<InventoryResponse> restock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.restock(productId, quantity));
    }
}
