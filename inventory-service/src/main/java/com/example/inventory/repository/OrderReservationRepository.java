package com.example.inventory.repository;

import com.example.inventory.entity.OrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderReservationRepository extends JpaRepository<OrderReservation, UUID> {
    Optional<OrderReservation> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
}