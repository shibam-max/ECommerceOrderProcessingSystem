package com.ecommerce.order.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Base class for all order domain events.
 * Follows the Observer pattern via Spring's ApplicationEvent infrastructure.
 * This decouples side-effects (audit logging, notifications) from core business logic.
 */
public abstract class OrderEvent extends ApplicationEvent {

    private final Long orderId;
    private final LocalDateTime occurredAt;

    protected OrderEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
