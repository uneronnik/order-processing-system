package com.example.inventory.service;

import com.example.common.event.InventoryReservedEvent;
import com.example.common.event.OrderCreatedEvent;
import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.entity.InventoryItem;
import com.example.inventory.exceptions.InsufficientStockException;
import com.example.inventory.exceptions.ProductAlreadyExistsException;
import com.example.inventory.exceptions.ProductNotFoundException;
import com.example.inventory.producer.InventoryKafkaProducer;
import com.example.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryKafkaProducer kafkaProducer;

    public InventoryService(InventoryRepository inventoryRepository, InventoryKafkaProducer kafkaProducer) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public void reserveItem(OrderCreatedEvent event) {
        InventoryItem item = inventoryRepository.findByProductId(event.productId())
                .orElseThrow(() -> new ProductNotFoundException(event.productId()));

        int available = item.getTotalQuantity() - item.getReservedQuantity();
        if (available < event.quantity()) {
            throw new InsufficientStockException(event.productId(), event.quantity(), available);
        }

        item.setReservedQuantity(item.getReservedQuantity() + event.quantity());
        inventoryRepository.save(item);

        BigDecimal amount = item.getPrice().multiply(BigDecimal.valueOf(event.quantity()));

        kafkaProducer.sendReservationSuccess(new InventoryReservedEvent(
                event.orderId(),
                event.productId(),
                event.quantity(),
                amount,
                LocalDateTime.now()
        ));
    }

    public InventoryResponse createItem(CreateInventoryItemRequest request) {
        if (inventoryRepository.findByProductId(request.productId()).isPresent()) {
            throw new ProductAlreadyExistsException(request.productId());
        }

        InventoryItem item = new InventoryItem();
        item.setProductId(request.productId());
        item.setName(request.name());
        item.setTotalQuantity(request.totalQuantity());
        item.setReservedQuantity(0);
        item.setPrice(request.price());

        return InventoryResponse.from(inventoryRepository.save(item));
    }

    public InventoryResponse getByProductId(String productId) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return InventoryResponse.from(item);
    }

    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    public InventoryResponse restock(String productId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        item.setTotalQuantity(item.getTotalQuantity() + quantity);
        return InventoryResponse.from(inventoryRepository.save(item));
    }
}
