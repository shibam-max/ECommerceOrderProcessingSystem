package com.ecommerce.order.model;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Determines whether a transition from the current status to the target is allowed.
     * Valid transitions:
     *   PENDING     -> PROCESSING, CANCELLED
     *   PROCESSING  -> SHIPPED
     *   SHIPPED     -> DELIVERED
     */
    public boolean canTransitionTo(OrderStatus target) {
        switch (this) {
            case PENDING:
                return target == PROCESSING || target == CANCELLED;
            case PROCESSING:
                return target == SHIPPED;
            case SHIPPED:
                return target == DELIVERED;
            case DELIVERED:
            case CANCELLED:
            default:
                return false;
        }
    }
}
