package com.ecommerce.order.audit;

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

import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class OrderAuditIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderAuditRepository auditRepository;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        auditRepository.deleteAll();
        orderRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    private Long createOrder() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Audit Tester")
                .customerEmail("audit@example.com")
                .items(Collections.singletonList(
                        OrderItemRequest.builder()
                                .productName("Book")
                                .quantity(1)
                                .unitPrice(new BigDecimal("20.00"))
                                .build()))
                .build();

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test
    @DisplayName("should record audit log on order creation")
    void shouldAuditCreation() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/orders/{id}/audit", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[0].newStatus").value("PENDING"));
    }

    @Test
    @DisplayName("should record full audit trail through lifecycle")
    void shouldAuditFullLifecycle() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateStatusRequest(OrderStatus.PROCESSING))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateStatusRequest(OrderStatus.SHIPPED))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}/audit", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].oldStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].newStatus").value("PROCESSING"))
                .andExpect(jsonPath("$[2].oldStatus").value("PROCESSING"))
                .andExpect(jsonPath("$[2].newStatus").value("SHIPPED"));
    }

    @Test
    @DisplayName("should record cancellation in audit trail")
    void shouldAuditCancellation() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}/audit", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].eventType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].newStatus").value("CANCELLED"))
                .andExpect(jsonPath("$[1].detail").value("Order cancelled by customer"));
    }
}
