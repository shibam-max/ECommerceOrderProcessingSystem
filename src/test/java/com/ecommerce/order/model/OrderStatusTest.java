package com.ecommerce.order.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be {2}")
    @DisplayName("should validate allowed status transitions")
    @CsvSource({
            "PENDING, PROCESSING, true",
            "PENDING, CANCELLED, true",
            "PENDING, SHIPPED, false",
            "PENDING, DELIVERED, false",
            "PROCESSING, SHIPPED, true",
            "PROCESSING, PENDING, false",
            "PROCESSING, DELIVERED, false",
            "PROCESSING, CANCELLED, false",
            "SHIPPED, DELIVERED, true",
            "SHIPPED, PENDING, false",
            "SHIPPED, PROCESSING, false",
            "SHIPPED, CANCELLED, false",
            "DELIVERED, PENDING, false",
            "DELIVERED, PROCESSING, false",
            "DELIVERED, SHIPPED, false",
            "DELIVERED, CANCELLED, false",
            "CANCELLED, PENDING, false",
            "CANCELLED, PROCESSING, false",
    })
    void shouldValidateTransitions(OrderStatus from, OrderStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    @DisplayName("terminal states should not transition to anything")
    void terminalStatesShouldNotTransition() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.DELIVERED.canTransitionTo(target)).isFalse();
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}
