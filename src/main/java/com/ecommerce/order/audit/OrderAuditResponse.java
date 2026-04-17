package com.ecommerce.order.audit;

import com.ecommerce.order.model.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAuditResponse {

    private Long id;
    private Long orderId;
    private String eventType;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private String detail;
    private LocalDateTime occurredAt;

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
}
