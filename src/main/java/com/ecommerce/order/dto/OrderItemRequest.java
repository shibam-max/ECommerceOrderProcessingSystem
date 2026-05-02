package com.ecommerce.order.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OrderItemRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;

    public OrderItemRequest() {}

    public OrderItemRequest(String productName, Integer quantity, BigDecimal unitPrice) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;

        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }

        public OrderItemRequest build() {
            return new OrderItemRequest(productName, quantity, unitPrice);
        }
    }
}
