package com.ecommerce.order.audit;

import com.ecommerce.order.model.OrderStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit record capturing every order lifecycle event.
 * Enables full traceability: who changed what, when, and why.
 * This is an Event Sourcing-lite approach — we don't replay events
 * to rebuild state, but we retain a complete history for audit/debugging.
 */
@Entity
@Table(name = "order_audit_log")
public class OrderAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private OrderStatus newStatus;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected OrderAuditLog() {}

    public OrderAuditLog(Long id, Long orderId, String eventType, OrderStatus oldStatus,
                         OrderStatus newStatus, String detail, LocalDateTime occurredAt) {
        this.id = id;
        this.orderId = orderId;
        this.eventType = eventType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
    public OrderStatus getOldStatus() { return oldStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
    public String getDetail() { return detail; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long orderId;
        private String eventType;
        private OrderStatus oldStatus;
        private OrderStatus newStatus;
        private String detail;
        private LocalDateTime occurredAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderId(Long orderId) { this.orderId = orderId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder oldStatus(OrderStatus oldStatus) { this.oldStatus = oldStatus; return this; }
        public Builder newStatus(OrderStatus newStatus) { this.newStatus = newStatus; return this; }
        public Builder detail(String detail) { this.detail = detail; return this; }
        public Builder occurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; return this; }

        public OrderAuditLog build() {
            return new OrderAuditLog(id, orderId, eventType, oldStatus, newStatus, detail, occurredAt);
        }
    }
}
