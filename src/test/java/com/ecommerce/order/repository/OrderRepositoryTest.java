package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    private Order createSampleOrder(OrderStatus status) {
        Order order = Order.builder()
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();
        order.addItem(item);

        return orderRepository.save(order);
    }

    @Test
    @DisplayName("should save and retrieve order with items")
    void shouldSaveAndRetrieve() {
        Order saved = createSampleOrder(OrderStatus.PENDING);

        Optional<Order> found = orderRepository.findByIdWithItems(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getCustomerName()).isEqualTo("Test Customer");
    }

    @Test
    @DisplayName("should find orders by status")
    void shouldFindByStatus() {
        createSampleOrder(OrderStatus.PENDING);
        createSampleOrder(OrderStatus.PENDING);
        createSampleOrder(OrderStatus.PROCESSING);

        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> processingOrders = orderRepository.findByStatus(OrderStatus.PROCESSING);

        assertThat(pendingOrders).hasSize(2);
        assertThat(processingOrders).hasSize(1);
    }

    @Test
    @DisplayName("should bulk update PENDING orders to PROCESSING")
    void shouldBulkUpdate() {
        createSampleOrder(OrderStatus.PENDING);
        createSampleOrder(OrderStatus.PENDING);
        createSampleOrder(OrderStatus.SHIPPED);

        int updated = orderRepository.bulkUpdatePendingToProcessing();

        assertThat(updated).isEqualTo(2);

        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> processing = orderRepository.findByStatus(OrderStatus.PROCESSING);

        assertThat(pending).isEmpty();
        assertThat(processing).hasSize(2);
    }

    @Test
    @DisplayName("should return 0 when no PENDING orders to promote")
    void shouldReturnZeroWhenNoPending() {
        createSampleOrder(OrderStatus.SHIPPED);

        int updated = orderRepository.bulkUpdatePendingToProcessing();

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("should return empty when order not found")
    void shouldReturnEmptyForMissingOrder() {
        Optional<Order> found = orderRepository.findByIdWithItems(9999L);
        assertThat(found).isEmpty();
    }
}
