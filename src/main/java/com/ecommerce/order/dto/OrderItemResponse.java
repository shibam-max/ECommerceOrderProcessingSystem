package com.ecommerce.order.dto;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public OrderItemResponse() {}

    public OrderItemResponse(Long id, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder lineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; return this; }

        public OrderItemResponse build() {
            return new OrderItemResponse(id, productName, quantity, unitPrice, lineTotal);
        }
    }
}
