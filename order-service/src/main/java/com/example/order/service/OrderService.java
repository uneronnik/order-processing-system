package com.example.order.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.producer.OrderKafkaProducer;
import com.example.order.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;

    public OrderService(OrderRepository orderRepository, OrderKafkaProducer orderKafkaProducer) {
        this.orderRepository = orderRepository;
        this.orderKafkaProducer = orderKafkaProducer;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Order order = new Order();
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setUserEmail(email);

        Order saved = orderRepository.save(order);

        orderKafkaProducer.sendOrderCreated(new OrderCreatedEvent(
                saved.getId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getCreatedAt()
        ));

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Order order = orderRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status, String reason, BigDecimal amount) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            if (amount != null) {
                order.setAmount(amount);
            }
            orderRepository.save(order);
        });
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(OrderStatus status) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByUserEmailAndStatus(email, status);
        } else {
            orders = orderRepository.findByUserEmail(email);
        }

        return orders.stream().map(OrderResponse::from).toList();
    }

}