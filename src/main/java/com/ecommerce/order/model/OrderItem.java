package com.ecommerce.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    public OrderItem() {}

    public OrderItem(String productName, Integer quantity, BigDecimal unitPrice, Order order) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Order order;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder order(Order order) { this.order = order; return this; }

        public OrderItem build() {
            OrderItem item = new OrderItem(productName, quantity, unitPrice, order);
            item.setId(id);
            return item;
        }
    }
}
