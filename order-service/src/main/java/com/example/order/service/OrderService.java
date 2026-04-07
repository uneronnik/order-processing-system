package com.example.order.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.producer.OrderKafkaProducer;
import com.example.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Order order = new Order();
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setAmount(request.amount());
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

        orderKafkaProducer.sendOrderCreated(new OrderCreatedEvent(
                saved.getId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getAmount(),
                saved.getCreatedAt()
        ));

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
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
}