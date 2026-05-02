package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String customerName;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public OrderResponse() {}

    public OrderResponse(Long id, String customerName, String customerEmail, OrderStatus status, 
                        BigDecimal totalAmount, LocalDateTime createdAt, LocalDateTime updatedAt, List<OrderItemResponse> items) {
        this.id = id;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String customerName;
        private String customerEmail;
        private OrderStatus status;
        private BigDecimal totalAmount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OrderItemResponse> items;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder customerEmail(String customerEmail) { this.customerEmail = customerEmail; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder items(List<OrderItemResponse> items) { this.items = items; return this; }

        public OrderResponse build() {
            return new OrderResponse(id, customerName, customerEmail, status, totalAmount, createdAt, updatedAt, items);
        }
    }
}
