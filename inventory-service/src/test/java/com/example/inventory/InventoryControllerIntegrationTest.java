package com.example.inventory;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.repository.InventoryRepository;
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

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class InventoryControllerIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InventoryRepository inventoryRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        inventoryRepository.deleteAll();
    }

    @Test
    void createItem_validRequest_returns201() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "PHONE-001", "Phone", 10, new BigDecimal("50000"));

        mockMvc.perform(post("/api/inventory")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PHONE-001"))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.totalQuantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(10))
                .andExpect(jsonPath("$.price").value(50000));
    }

    @Test
    void createItem_duplicateProductId_returns409() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "PHONE-001", "Phone", 10, new BigDecimal("50000"));

        mockMvc.perform(post("/api/inventory")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createItem_blankProductId_returns400() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "", "Phone", 10, new BigDecimal("50000"));

        mockMvc.perform(post("/api/inventory")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByProductId_existing_returns200() throws Exception {
        createTestItem("LAPTOP-001", "Laptop", 5, new BigDecimal("99990"));

        mockMvc.perform(get("/api/inventory/LAPTOP-001")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("LAPTOP-001"))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getByProductId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/UNKNOWN")
                        .with(user("user").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void restock_existingProduct_updatesQuantity() throws Exception {
        createTestItem("PHONE-001", "Phone", 10, new BigDecimal("50000"));

        mockMvc.perform(put("/api/inventory/PHONE-001/restock")
                        .with(user("admin").roles("ADMIN"))
                        .param("quantity", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(25));
    }

    @Test
    void restock_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/inventory/UNKNOWN/restock")
                        .with(user("admin").roles("ADMIN"))
                        .param("quantity", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsAllItems() throws Exception {
        createTestItem("PHONE-001", "Phone", 10, new BigDecimal("50000"));
        createTestItem("LAPTOP-001", "Laptop", 5, new BigDecimal("99990"));

        mockMvc.perform(get("/api/inventory")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByProductId_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/inventory/PHONE-001"))
                .andExpect(status().isForbidden());
    }

    private void createTestItem(String productId, String name, int quantity, BigDecimal price) throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                productId, name, quantity, price);

        mockMvc.perform(post("/api/inventory")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}