package com.ecommerce.order.event;

import com.ecommerce.order.audit.OrderAuditLog;
import com.ecommerce.order.audit.OrderAuditRepository;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private OrderAuditRepository auditRepository;

    @InjectMocks
    private OrderEventListener listener;

    @Test
    @DisplayName("should create audit log on OrderCreatedEvent")
    void shouldAuditOrderCreation() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                this, 1L, "alice@example.com", 3, new BigDecimal("150.00"));

        listener.onOrderCreated(event);

        ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditRepository).save(captor.capture());
        OrderAuditLog log = captor.getValue();
        assertThat(log.getOrderId()).isEqualTo(1L);
        assertThat(log.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(log.getNewStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(log.getDetail()).contains("3 item(s)").contains("150.00");
    }

    @Test
    @DisplayName("should create audit log on OrderStatusChangedEvent")
    void shouldAuditStatusChange() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                this, 5L, OrderStatus.PENDING, OrderStatus.PROCESSING,
                "Status updated from PENDING to PROCESSING");

        listener.onOrderStatusChanged(event);

        ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditRepository).save(captor.capture());
        OrderAuditLog log = captor.getValue();
        assertThat(log.getOrderId()).isEqualTo(5L);
        assertThat(log.getEventType()).isEqualTo("STATUS_CHANGED");
        assertThat(log.getOldStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(log.getNewStatus()).isEqualTo(OrderStatus.PROCESSING);
    }
}
