package com.ecommerce.order.event;

import com.ecommerce.order.model.OrderStatus;

public class OrderStatusChangedEvent extends OrderEvent {

    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final String reason;

    public OrderStatusChangedEvent(Object source, Long orderId,
                                   OrderStatus previousStatus, OrderStatus newStatus,
                                   String reason) {
        super(source, orderId);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public OrderStatus getPreviousStatus() { return previousStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
}
