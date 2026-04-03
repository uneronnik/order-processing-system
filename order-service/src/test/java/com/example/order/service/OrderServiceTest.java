package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldReturnCreatedOrder() {
        CreateOrderRequest request = new CreateOrderRequest("LAPTOP-001", 1, new BigDecimal("79990.00"));
        Order saved = new Order();
        saved.setId(UUID.randomUUID());
        saved.setProductId("LAPTOP-001");
        saved.setQuantity(1);
        saved.setAmount(new BigDecimal("79990.00"));
        saved.setStatus(OrderStatus.PENDING);

        when(orderRepository.save(any())).thenReturn(saved);

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response.id());
        assertEquals("LAPTOP-001", response.productId());
        assertEquals(OrderStatus.PENDING, response.status());
        verify(orderRepository).save(any());
    }

    @Test
    void getOrder_whenNotFound_shouldThrowException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(id));
    }
}