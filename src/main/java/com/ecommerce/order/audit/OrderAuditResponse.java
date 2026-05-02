package com.ecommerce.order.audit;

import com.ecommerce.order.model.OrderStatus;

import java.time.LocalDateTime;

public class OrderAuditResponse {

    private Long id;
    private Long orderId;
    private String eventType;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private String detail;
    private LocalDateTime occurredAt;

    public OrderAuditResponse() {}

    public OrderAuditResponse(Long id, Long orderId, String eventType, OrderStatus oldStatus, 
                            OrderStatus newStatus, String detail, LocalDateTime occurredAt) {
        this.id = id;
        this.orderId = orderId;
        this.eventType = eventType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public OrderStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(OrderStatus oldStatus) { this.oldStatus = oldStatus; }

    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public static Builder builder() { return new Builder(); }

    public static OrderAuditResponse fromEntity(OrderAuditLog log) {
        return OrderAuditResponse.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .eventType(log.getEventType())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .detail(log.getDetail())
                .occurredAt(log.getOccurredAt())
                .build();
    }

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

        public OrderAuditResponse build() {
            return new OrderAuditResponse(id, orderId, eventType, oldStatus, newStatus, detail, occurredAt);
        }
    }
}
