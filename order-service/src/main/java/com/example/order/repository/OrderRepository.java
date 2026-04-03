package com.example.order.repository;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStatus(OrderStatus status);
}