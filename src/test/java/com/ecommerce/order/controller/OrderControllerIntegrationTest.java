package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.UpdateStatusRequest;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    private CreateOrderRequest validCreateRequest() {
        return CreateOrderRequest.builder()
                .customerName("Alice Wonderland")
                .customerEmail("alice@example.com")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productName("Laptop")
                                .quantity(1)
                                .unitPrice(new BigDecimal("999.99"))
                                .build(),
                        OrderItemRequest.builder()
                                .productName("Mouse")
                                .quantity(2)
                                .unitPrice(new BigDecimal("25.50"))
                                .build()
                ))
                .build();
    }

    private Long createOrderAndReturnId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrder {

        @Test
        @DisplayName("should create order and return 201 with correct total")
        void shouldCreateOrder() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.customerName").value("Alice Wonderland"))
                    .andExpect(jsonPath("$.customerEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(1050.99))
                    .andExpect(jsonPath("$.items", hasSize(2)))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when customer name is blank")
        void shouldRejectBlankName() throws Exception {
            CreateOrderRequest request = validCreateRequest();
            request.setCustomerName("");

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.customerName").exists());
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldRejectInvalidEmail() throws Exception {
            CreateOrderRequest request = validCreateRequest();
            request.setCustomerEmail("not-an-email");

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.customerEmail").exists());
        }

        @Test
        @DisplayName("should return 400 when items list is empty")
        void shouldRejectEmptyItems() throws Exception {
            CreateOrderRequest request = validCreateRequest();
            request.setItems(Collections.emptyList());

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.items").exists());
        }

        @Test
        @DisplayName("should return 400 when item quantity is zero")
        void shouldRejectZeroQuantity() throws Exception {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("Bob")
                    .customerEmail("bob@example.com")
                    .items(Collections.singletonList(
                            OrderItemRequest.builder()
                                    .productName("Widget")
                                    .quantity(0)
                                    .unitPrice(new BigDecimal("10.00"))
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists());
        }

        @Test
        @DisplayName("should return 400 for malformed JSON body")
        void shouldRejectMalformedJson() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrder {

        @Test
        @DisplayName("should return order by ID")
        void shouldReturnOrder() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(get("/api/orders/{id}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderId))
                    .andExpect(jsonPath("$.items", hasSize(2)));
        }

        @Test
        @DisplayName("should return 404 for non-existent order")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/orders/{id}", 99999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("99999")));
        }
    }

    @Nested
    @DisplayName("GET /api/orders")
    class ListOrders {

        @Test
        @DisplayName("should return all orders")
        void shouldReturnAll() throws Exception {
            createOrderAndReturnId();
            createOrderAndReturnId();

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should return only orders matching status filter")
        void shouldFilterByStatus() throws Exception {
            createOrderAndReturnId();

            mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            mockMvc.perform(get("/api/orders").param("status", "SHIPPED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return empty list when no orders exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/status")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should update PENDING to PROCESSING")
        void shouldUpdateStatus() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.PROCESSING))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        }

        @Test
        @DisplayName("should transition through full lifecycle")
        void shouldTransitionFullLifecycle() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.PROCESSING))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.SHIPPED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.DELIVERED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELIVERED"));
        }

        @Test
        @DisplayName("should return 409 for invalid transition")
        void shouldRejectInvalidTransition() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.DELIVERED))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            containsString("Cannot transition")));
        }

        @Test
        @DisplayName("should return 404 for non-existent order")
        void shouldReturn404() throws Exception {
            mockMvc.perform(patch("/api/orders/{id}/status", 99999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.PROCESSING))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("should cancel PENDING order and return CANCELLED status")
        void shouldCancelPendingOrder() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 409 when cancelling non-PENDING order")
        void shouldRejectCancellingProcessingOrder() throws Exception {
            Long orderId = createOrderAndReturnId();

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateStatusRequest(OrderStatus.PROCESSING))))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            containsString("PENDING")));
        }

        @Test
        @DisplayName("should return 404 for non-existent order")
        void shouldReturn404() throws Exception {
            mockMvc.perform(post("/api/orders/{id}/cancel", 99999))
                    .andExpect(status().isNotFound());
        }
    }
}
