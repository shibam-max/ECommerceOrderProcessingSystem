package com.ecommerce.order.validation;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.exception.InvalidOrderStateException;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: Limits the number of line items per order.
 * In production, the threshold would come from configuration or a rules engine.
 */
@Component
public class MaxItemsPerOrderValidator implements OrderValidationStrategy {

    private static final int MAX_ITEMS = 50;

    @Override
    public void validate(CreateOrderRequest request) {
        if (request.getItems().size() > MAX_ITEMS) {
            throw new InvalidOrderStateException(
                    String.format("Order cannot exceed %d items (received %d)",
                            MAX_ITEMS, request.getItems().size()));
        }
    }
}
