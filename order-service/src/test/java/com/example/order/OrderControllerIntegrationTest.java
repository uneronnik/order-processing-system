package com.example.order;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderRepository orderRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_validRequest_returns201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 2);

        mockMvc.perform(post("/api/orders")
                        .with(user("user@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PHONE-001"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userEmail").value("user@test.com"));
    }

    @Test
    void createOrder_blankProductId_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("", 2);

        mockMvc.perform(post("/api/orders")
                        .with(user("user@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_zeroQuantity_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 0);

        mockMvc.perform(post("/api/orders")
                        .with(user("user@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_ownOrder_returns200() throws Exception {
        // создаём заказ
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 1);
        String response = mockMvc.perform(post("/api/orders")
                        .with(user("user@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(response).get("id").asText();

        // получаем его
        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(user("user@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PHONE-001"));
    }

    @Test
    void getOrder_otherUsersOrder_returns404() throws Exception {
        // создаём заказ от одного пользователя
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 1);
        String response = mockMvc.perform(post("/api/orders")
                        .with(user("user1@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(response).get("id").asText();

        // пытаемся получить от другого
        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(user("user2@test.com").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID())
                        .with(user("user@test.com").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyOrders_returnsOnlyOwnOrders() throws Exception {
        // создаём заказы от двух пользователей
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 1);
        mockMvc.perform(post("/api/orders")
                        .with(user("user1@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .with(user("user2@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // user1 видит только свой
        mockMvc.perform(get("/api/orders")
                        .with(user("user1@test.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllOrders_admin_returnsAll() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("PHONE-001", 1);
        mockMvc.perform(post("/api/orders")
                        .with(user("user1@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .with(user("user2@test.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/admin/all")
                        .with(user("admin@admin.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}