package com.ecommerce.order.event;

import java.math.BigDecimal;

public class OrderCreatedEvent extends OrderEvent {

    private final String customerEmail;
    private final int itemCount;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Object source, Long orderId, String customerEmail,
                             int itemCount, BigDecimal totalAmount) {
        super(source, orderId);
        this.customerEmail = customerEmail;
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
    }

    public String getCustomerEmail() { return customerEmail; }
    public int getItemCount() { return itemCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
