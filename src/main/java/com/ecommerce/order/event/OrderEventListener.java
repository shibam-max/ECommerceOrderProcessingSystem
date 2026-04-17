package com.ecommerce.order.event;

import com.ecommerce.order.audit.OrderAuditLog;
import com.ecommerce.order.audit.OrderAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Observer Pattern: Listens to domain events published by OrderService.
 *
 * Responsibilities decoupled from business logic:
 *   - Writes immutable audit trail records
 *   - Simulates customer notification (log-based; swap for email/SMS in production)
 *
 * Adding new side-effects (analytics, webhooks) means adding new @EventListener
 * methods here or in new listener classes — zero changes to OrderService.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderAuditRepository auditRepository;

    public OrderEventListener(OrderAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(event.getOrderId())
                .eventType("ORDER_CREATED")
                .newStatus(com.ecommerce.order.model.OrderStatus.PENDING)
                .detail(String.format("Order placed with %d item(s), total $%s",
                        event.getItemCount(), event.getTotalAmount()))
                .occurredAt(event.getOccurredAt())
                .build();
        auditRepository.save(auditLog);

        log.info("[NOTIFICATION] Order #{} confirmation sent to {}",
                event.getOrderId(), event.getCustomerEmail());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(event.getOrderId())
                .eventType("STATUS_CHANGED")
                .oldStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .detail(event.getReason())
                .occurredAt(event.getOccurredAt())
                .build();
        auditRepository.save(auditLog);

        log.info("[NOTIFICATION] Order #{} status update: {} -> {}",
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());
    }
}
