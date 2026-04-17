package com.ecommerce.order.validation;

import com.ecommerce.order.dto.CreateOrderRequest;

/**
 * Strategy Pattern: Defines a pluggable validation contract for order creation.
 *
 * Each implementation encapsulates one business rule. The service iterates
 * all registered strategies — adding a new rule means adding a new @Component,
 * with zero changes to existing code (Open/Closed Principle).
 */
public interface OrderValidationStrategy {

    void validate(CreateOrderRequest request);
}
