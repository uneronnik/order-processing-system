package com.example.inventory.service;

import com.example.common.event.InventoryReservedEvent;
import com.example.common.event.OrderCreatedEvent;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.OrderReservation;
import com.example.inventory.exceptions.InsufficientStockException;
import com.example.inventory.exceptions.ProductAlreadyExistsException;
import com.example.inventory.exceptions.ProductNotFoundException;
import com.example.inventory.producer.InventoryKafkaProducer;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.OrderReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderReservationRepository orderReservationRepository;

    @Mock
    private InventoryKafkaProducer kafkaProducer;

    @InjectMocks
    private InventoryService inventoryService;

    @Captor
    private ArgumentCaptor<InventoryItem> itemCaptor;

    @Captor
    private ArgumentCaptor<OrderReservation> reservationCaptor;

    @Captor
    private ArgumentCaptor<InventoryReservedEvent> reservedEventCaptor;

    // ===================== reserveItem =====================

    @Test
    void reserveItem_success_updatesReservedQuantity() {
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 0, new BigDecimal("50000"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), "PHONE-001", 3, LocalDateTime.now());

        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderReservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserveItem(event);

        verify(inventoryRepository).save(itemCaptor.capture());
        assertEquals(3, itemCaptor.getValue().getReservedQuantity());
    }

    @Test
    void reserveItem_success_savesOrderReservation() {
        UUID orderId = UUID.randomUUID();
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 0, new BigDecimal("50000"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, "PHONE-001", 2, LocalDateTime.now());

        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderReservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserveItem(event);

        verify(orderReservationRepository).save(reservationCaptor.capture());
        OrderReservation saved = reservationCaptor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertEquals("PHONE-001", saved.getProductId());
        assertEquals(2, saved.getQuantity());
    }

    @Test
    void reserveItem_success_sendsEventWithCorrectAmount() {
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 0, new BigDecimal("50000"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), "PHONE-001", 3, LocalDateTime.now());

        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderReservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserveItem(event);

        verify(kafkaProducer).sendReservationSuccess(reservedEventCaptor.capture());
        InventoryReservedEvent sent = reservedEventCaptor.getValue();
        assertEquals(new BigDecimal("150000"), sent.amount()); // 50000 × 3
        assertEquals("PHONE-001", sent.productId());
        assertEquals(3, sent.quantity());
    }

    @Test
    void reserveItem_productNotFound_throwsException() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), "UNKNOWN", 1, LocalDateTime.now());

        when(inventoryRepository.findByProductId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
                inventoryService.reserveItem(event));

        verify(kafkaProducer, never()).sendReservationSuccess(any());
    }

    @Test
    void reserveItem_insufficientStock_throwsException() {
        InventoryItem item = createItem("PHONE-001", "Phone", 5, 3, new BigDecimal("50000"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), "PHONE-001", 5, LocalDateTime.now());

        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));

        // available = 5 - 3 = 2, requesting 5
        assertThrows(InsufficientStockException.class, () ->
                inventoryService.reserveItem(event));

        verify(inventoryRepository, never()).save(any());
        verify(kafkaProducer, never()).sendReservationSuccess(any());
    }

    // ===================== createItem =====================

    @Test
    void createItem_success_savesAndReturnsResponse() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "LAPTOP-001", "Laptop", 20, new BigDecimal("99990"));

        when(inventoryRepository.findByProductId("LAPTOP-001")).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse response = inventoryService.createItem(request);

        verify(inventoryRepository).save(itemCaptor.capture());
        InventoryItem saved = itemCaptor.getValue();
        assertEquals("LAPTOP-001", saved.getProductId());
        assertEquals("Laptop", saved.getName());
        assertEquals(20, saved.getTotalQuantity());
        assertEquals(0, saved.getReservedQuantity());
        assertEquals(new BigDecimal("99990"), saved.getPrice());
    }

    @Test
    void createItem_duplicateProductId_throwsException() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "PHONE-001", "Phone", 10, new BigDecimal("50000"));
        InventoryItem existing = createItem("PHONE-001", "Phone", 10, 0, new BigDecimal("50000"));

        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(existing));

        assertThrows(ProductAlreadyExistsException.class, () ->
                inventoryService.createItem(request));

        verify(inventoryRepository, never()).save(any());
    }

    // ===================== getByProductId =====================

    @Test
    void getByProductId_existing_returnsResponse() {
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 2, new BigDecimal("50000"));
        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));

        InventoryResponse response = inventoryService.getByProductId("PHONE-001");

        assertNotNull(response);
    }

    @Test
    void getByProductId_notFound_throwsException() {
        when(inventoryRepository.findByProductId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
                inventoryService.getByProductId("UNKNOWN"));
    }

    // ===================== restock =====================

    @Test
    void restock_addsTotalQuantity() {
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 0, new BigDecimal("50000"));
        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.restock("PHONE-001", 15);

        verify(inventoryRepository).save(itemCaptor.capture());
        assertEquals(25, itemCaptor.getValue().getTotalQuantity());
    }

    @Test
    void restock_productNotFound_throwsException() {
        when(inventoryRepository.findByProductId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
                inventoryService.restock("UNKNOWN", 10));
    }

    // ===================== confirmReservation =====================

    @Test
    void confirmReservation_decreasesBothQuantities() {
        UUID orderId = UUID.randomUUID();
        OrderReservation reservation = createReservation(orderId, "PHONE-001", 3);
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 3, new BigDecimal("50000"));

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, "PHONE-001", 3, new BigDecimal("150000"), LocalDateTime.now());

        when(orderReservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.confirmReservation(event);

        verify(inventoryRepository).save(itemCaptor.capture());
        InventoryItem saved = itemCaptor.getValue();
        assertEquals(7, saved.getTotalQuantity());   // 10 - 3
        assertEquals(0, saved.getReservedQuantity()); // 3 - 3
    }

    @Test
    void confirmReservation_noReservation_doesNothing() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, "PHONE-001", 3, new BigDecimal("150000"), LocalDateTime.now());

        when(orderReservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        inventoryService.confirmReservation(event);

        verify(inventoryRepository, never()).save(any());
    }

    // ===================== cancelReservation =====================

    @Test
    void cancelReservation_decreasesOnlyReservedQuantity() {
        UUID orderId = UUID.randomUUID();
        OrderReservation reservation = createReservation(orderId, "PHONE-001", 3);
        InventoryItem item = createItem("PHONE-001", "Phone", 10, 3, new BigDecimal("50000"));

        PaymentFailedEvent event = new PaymentFailedEvent(
                orderId, "Insufficient funds", "PHONE-001", 3, LocalDateTime.now());

        when(orderReservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductId("PHONE-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.cancelReservation(event);

        verify(inventoryRepository).save(itemCaptor.capture());
        InventoryItem saved = itemCaptor.getValue();
        assertEquals(10, saved.getTotalQuantity());   // не изменилось
        assertEquals(0, saved.getReservedQuantity()); // 3 - 3
    }

    @Test
    void cancelReservation_noReservation_doesNothing() {
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                orderId, "Insufficient funds", "PHONE-001", 3, LocalDateTime.now());

        when(orderReservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        inventoryService.cancelReservation(event);

        verify(inventoryRepository, never()).save(any());
    }

    // ===================== helpers =====================

    private InventoryItem createItem(String productId, String name, int total, int reserved, BigDecimal price) {
        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setName(name);
        item.setTotalQuantity(total);
        item.setReservedQuantity(reserved);
        item.setPrice(price);
        return item;
    }

    private OrderReservation createReservation(UUID orderId, String productId, int quantity) {
        OrderReservation reservation = new OrderReservation();
        reservation.setOrderId(orderId);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setReservedAt(LocalDateTime.now());
        return reservation;
    }
}