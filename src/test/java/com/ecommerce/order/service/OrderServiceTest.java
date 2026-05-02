package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderStatusChangedEvent;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.validation.OrderValidationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        List<OrderValidationStrategy> strategies = Collections.emptyList();
        orderService = new OrderService(orderRepository, eventPublisher, strategies);

        sampleOrder = Order.builder()
                .id(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .productName("Widget")
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .order(sampleOrder)
                .build();
        sampleOrder.getItems().add(item);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should create order and publish OrderCreatedEvent")
        void shouldCreateOrder() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .items(Collections.singletonList(
                            OrderItemRequest.builder()
                                    .productName("Widget")
                                    .quantity(2)
                                    .unitPrice(new BigDecimal("29.99"))
                                    .build()
                    ))
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getCustomerName()).isEqualTo("John Doe");
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getItems()).hasSize(1);

            ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            OrderCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getOrderId()).isEqualTo(1L);
            assertThat(event.getCustomerEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should create order with multiple items")
        void shouldCreateOrderWithMultipleItems() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("Jane Smith")
                    .customerEmail("jane@example.com")
                    .items(Arrays.asList(
                            OrderItemRequest.builder()
                                    .productName("Widget").quantity(1)
                                    .unitPrice(new BigDecimal("10.00")).build(),
                            OrderItemRequest.builder()
                                    .productName("Gadget").quantity(3)
                                    .unitPrice(new BigDecimal("5.00")).build()
                    ))
                    .build();

            Order multiItemOrder = Order.builder()
                    .id(2L)
                    .customerName("Jane Smith")
                    .customerEmail("jane@example.com")
                    .status(OrderStatus.PENDING)
                    .totalAmount(new BigDecimal("25.00"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            multiItemOrder.getItems().addAll(Arrays.asList(
                    OrderItem.builder().id(2L).productName("Widget").quantity(1)
                            .unitPrice(new BigDecimal("10.00")).order(multiItemOrder).build(),
                    OrderItem.builder().id(3L).productName("Gadget").quantity(3)
                            .unitPrice(new BigDecimal("5.00")).order(multiItemOrder).build()
            ));

            when(orderRepository.save(any(Order.class))).thenReturn(multiItemOrder);

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getTotalAmount()).isEqualByComparingTo("25.00");
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrder() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            OrderResponse response = orderService.getOrder(1L);
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCustomerEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when order does not exist")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.getOrder(999L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrders {

        @Test
        @DisplayName("should return all orders when no filter provided")
        void shouldReturnAllOrders() {
            when(orderRepository.findAll()).thenReturn(Collections.singletonList(sampleOrder));
            List<OrderResponse> responses = orderService.listOrders(null);
            assertThat(responses).hasSize(1);
            verify(orderRepository).findAll();
            verify(orderRepository, never()).findByStatus(any());
        }

        @Test
        @DisplayName("should return filtered orders by status")
        void shouldFilterByStatus() {
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Collections.singletonList(sampleOrder));
            List<OrderResponse> responses = orderService.listOrders(OrderStatus.PENDING);
            assertThat(responses).hasSize(1);
            verify(orderRepository).findByStatus(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("should return empty list when no orders match")
        void shouldReturnEmptyList() {
            when(orderRepository.findByStatus(OrderStatus.SHIPPED)).thenReturn(Collections.emptyList());
            List<OrderResponse> responses = orderService.listOrders(OrderStatus.SHIPPED);
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should transition PENDING to PROCESSING and publish event")
        void shouldTransitionPendingToProcessing() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(1L,
                    new UpdateStatusRequest(OrderStatus.PROCESSING));

            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);

            ArgumentCaptor<OrderStatusChangedEvent> captor =
                    ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(captor.getValue().getNewStatus()).isEqualTo(OrderStatus.PROCESSING);
        }

        @Test
        @DisplayName("should reject invalid transition PENDING to SHIPPED")
        void shouldRejectInvalidTransition() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L,
                    new UpdateStatusRequest(OrderStatus.SHIPPED)))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("SHIPPED");
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should reject transition from DELIVERED")
        void shouldRejectTransitionFromDelivered() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L,
                    new UpdateStatusRequest(OrderStatus.PROCESSING)))
                    .isInstanceOf(InvalidOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel PENDING order and publish event")
        void shouldCancelPendingOrder() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.cancelOrder(1L);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
        }

        @Test
        @DisplayName("should reject cancellation of PROCESSING order")
        void shouldRejectCancellationOfProcessingOrder() {
            sampleOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should reject cancellation of SHIPPED order")
        void shouldRejectCancellationOfShippedOrder() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("promotePendingOrders")
    class PromotePendingOrders {

        @Test
        @DisplayName("should delegate to repository bulk update")
        void shouldDelegateToBulkUpdate() {
            when(orderRepository.bulkUpdatePendingToProcessing()).thenReturn(3);
            int count = orderService.promotePendingOrders();
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero when no pending orders exist")
        void shouldReturnZeroWhenNoPending() {
            when(orderRepository.bulkUpdatePendingToProcessing()).thenReturn(0);
            int count = orderService.promotePendingOrders();
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("getOrderInsights")
    class GetOrderInsights {

        @Test
        @DisplayName("should return aggregated order insights")
        void shouldReturnOrderInsights() {
            when(orderRepository.count()).thenReturn(10L);
            when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(3L);
            when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(2L);
            when(orderRepository.countByStatus(OrderStatus.SHIPPED)).thenReturn(2L);
            when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(2L);
            when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(1L);
            when(orderRepository.sumTotalAmount()).thenReturn(new BigDecimal("1500.00"));
            when(orderRepository.averageTotalAmount()).thenReturn(150.0);
            when(orderRepository.sumTotalAmountByStatus(OrderStatus.PENDING)).thenReturn(new BigDecimal("400.00"));

            OrderInsightsResponse response = orderService.getOrderInsights();

            assertThat(response.getTotalOrders()).isEqualTo(10L);
            assertThat(response.getPendingOrders()).isEqualTo(3L);
            assertThat(response.getProcessingOrders()).isEqualTo(2L);
            assertThat(response.getShippedOrders()).isEqualTo(2L);
            assertThat(response.getDeliveredOrders()).isEqualTo(2L);
            assertThat(response.getCancelledOrders()).isEqualTo(1L);
            assertThat(response.getTotalRevenue()).isEqualByComparingTo("1500.00");
            assertThat(response.getAverageOrderValue()).isEqualByComparingTo("150.0");
            assertThat(response.getPendingRevenue()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("should default aggregate amounts to zero when repository returns null")
        void shouldDefaultInsightsAmountsToZeroWhenNull() {
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(0L);
            when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(0L);
            when(orderRepository.countByStatus(OrderStatus.SHIPPED)).thenReturn(0L);
            when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(0L);
            when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(0L);
            when(orderRepository.sumTotalAmount()).thenReturn(null);
            when(orderRepository.averageTotalAmount()).thenReturn(null);
            when(orderRepository.sumTotalAmountByStatus(OrderStatus.PENDING)).thenReturn(null);

            OrderInsightsResponse response = orderService.getOrderInsights();

            assertThat(response.getTotalRevenue()).isEqualByComparingTo("0");
            assertThat(response.getAverageOrderValue()).isEqualByComparingTo("0");
            assertThat(response.getPendingRevenue()).isEqualByComparingTo("0");
        }
    }
}
