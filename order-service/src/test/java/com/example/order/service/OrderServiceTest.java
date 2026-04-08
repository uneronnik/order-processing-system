package com.example.order.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.producer.OrderKafkaProducer;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderKafkaProducer orderKafkaProducer;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of())
        );
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ===================== createOrder =====================

    @Test
    void createOrder_savesOrderWithCorrectFields() {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 2);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        orderService.createOrder(request);

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals("PHONE-001", saved.getProductId());
        assertEquals(2, saved.getQuantity());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals("user@test.com", saved.getUserEmail());
    }

    @Test
    void createOrder_sendsKafkaEvent() {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 3);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        orderService.createOrder(request);

        verify(orderKafkaProducer).sendOrderCreated(eventCaptor.capture());
        OrderCreatedEvent sent = eventCaptor.getValue();
        assertEquals("PHONE-001", sent.productId());
        assertEquals(3, sent.quantity());
        assertNotNull(sent.orderId());
    }

    @Test
    void createOrder_returnsResponseWithPendingStatus() {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 1);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertEquals("PHONE-001", response.productId());
        assertEquals(1, response.quantity());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals("user@test.com", response.userEmail());
    }

    // ===================== getOrder =====================

    @Test
    void getOrder_ownOrder_returnsResponse() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, "PHONE-001", 1, OrderStatus.PENDING, "user@test.com");
        when(orderRepository.findByIdAndUserEmail(orderId, "user@test.com")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId);

        assertEquals(orderId, response.id());
        assertEquals("PHONE-001", response.productId());
    }

    @Test
    void getOrder_notFound_throwsException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserEmail(orderId, "user@test.com")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(orderId));
    }

    @Test
    void getOrder_otherUsersOrder_throwsException() {
        UUID orderId = UUID.randomUUID();
        // findByIdAndUserEmail не найдёт чужой заказ — вернёт empty
        when(orderRepository.findByIdAndUserEmail(orderId, "user@test.com")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(orderId));
    }

    // ===================== updateStatus =====================

    @Test
    void updateStatus_existingOrder_updatesStatusAndAmount() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, "PHONE-001", 1, OrderStatus.PENDING, "user@test.com");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(orderId, OrderStatus.CONFIRMED, null, new BigDecimal("50000"));

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(OrderStatus.CONFIRMED, saved.getStatus());
        assertEquals(new BigDecimal("50000"), saved.getAmount());
    }

    @Test
    void updateStatus_nullAmount_doesNotOverwrite() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, "PHONE-001", 1, OrderStatus.PENDING, "user@test.com");
        order.setAmount(new BigDecimal("50000"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(orderId, OrderStatus.CANCELLED, "No stock", null);

        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(new BigDecimal("50000"), orderCaptor.getValue().getAmount());
        assertEquals(OrderStatus.CANCELLED, orderCaptor.getValue().getStatus());
    }

    @Test
    void updateStatus_orderNotFound_doesNothing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        orderService.updateStatus(orderId, OrderStatus.CANCELLED, null, null);

        verify(orderRepository, never()).save(any());
    }

    // ===================== getMyOrders =====================

    @Test
    void getMyOrders_noFilter_returnsAllUserOrders() {
        List<Order> orders = List.of(
                createOrder(UUID.randomUUID(), "PHONE-001", 1, OrderStatus.PENDING, "user@test.com"),
                createOrder(UUID.randomUUID(), "LAPTOP-001", 2, OrderStatus.CONFIRMED, "user@test.com")
        );
        when(orderRepository.findByUserEmail("user@test.com")).thenReturn(orders);

        List<OrderResponse> result = orderService.getMyOrders(null);

        assertEquals(2, result.size());
    }

    @Test
    void getMyOrders_withStatusFilter_returnsFiltered() {
        List<Order> orders = List.of(
                createOrder(UUID.randomUUID(), "PHONE-001", 1, OrderStatus.CONFIRMED, "user@test.com")
        );
        when(orderRepository.findByUserEmailAndStatus("user@test.com", OrderStatus.CONFIRMED)).thenReturn(orders);

        List<OrderResponse> result = orderService.getMyOrders(OrderStatus.CONFIRMED);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.CONFIRMED, result.get(0).status());
    }

    // ===================== getAllOrders =====================

    @Test
    void getAllOrders_noFilter_returnsAll() {
        List<Order> orders = List.of(
                createOrder(UUID.randomUUID(), "PHONE-001", 1, OrderStatus.PENDING, "user1@test.com"),
                createOrder(UUID.randomUUID(), "LAPTOP-001", 1, OrderStatus.CONFIRMED, "user2@test.com")
        );
        when(orderRepository.findAll()).thenReturn(orders);

        List<OrderResponse> result = orderService.getAllOrders(null);

        assertEquals(2, result.size());
    }

    @Test
    void getAllOrders_withStatusFilter_returnsFiltered() {
        List<Order> orders = List.of(
                createOrder(UUID.randomUUID(), "PHONE-001", 1, OrderStatus.CANCELLED, "user1@test.com")
        );
        when(orderRepository.findByStatus(OrderStatus.CANCELLED)).thenReturn(orders);

        List<OrderResponse> result = orderService.getAllOrders(OrderStatus.CANCELLED);

        assertEquals(1, result.size());
    }

    // ===================== helper =====================

    private Order createOrder(UUID id, String productId, int quantity, OrderStatus status, String email) {
        Order order = new Order();
        order.setId(id);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(status);
        order.setUserEmail(email);
        return order;
    }
}