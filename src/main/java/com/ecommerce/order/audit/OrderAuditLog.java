package com.ecommerce.order.audit;

import com.ecommerce.order.model.OrderStatus;
import lombok.*;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
