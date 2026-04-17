package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
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
        @DisplayName("should create order and return response with correct total")
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

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            Order saved = captor.getValue();
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("59.98");
        }

        @Test
        @DisplayName("should create order with multiple items")
        void shouldCreateOrderWithMultipleItems() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("Jane Smith")
                    .customerEmail("jane@example.com")
                    .items(Arrays.asList(
                            OrderItemRequest.builder()
                                    .productName("Widget")
                                    .quantity(1)
                                    .unitPrice(new BigDecimal("10.00"))
                                    .build(),
                            OrderItemRequest.builder()
                                    .productName("Gadget")
                                    .quantity(3)
                                    .unitPrice(new BigDecimal("5.00"))
                                    .build()
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
            verify(orderRepository, never()).findAll();
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
        @DisplayName("should transition PENDING to PROCESSING")
        void shouldTransitionPendingToProcessing() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(1L,
                    new UpdateStatusRequest(OrderStatus.PROCESSING));

            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
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
        @DisplayName("should cancel PENDING order successfully")
        void shouldCancelPendingOrder() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.cancelOrder(1L);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
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
            verify(orderRepository).bulkUpdatePendingToProcessing();
        }

        @Test
        @DisplayName("should return zero when no pending orders exist")
        void shouldReturnZeroWhenNoPending() {
            when(orderRepository.bulkUpdatePendingToProcessing()).thenReturn(0);

            int count = orderService.promotePendingOrders();

            assertThat(count).isZero();
        }
    }
}
