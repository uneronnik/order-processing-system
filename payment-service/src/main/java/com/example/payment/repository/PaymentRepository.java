package com.example.payment.repository;

import com.example.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByOrderId(UUID orderId);
}