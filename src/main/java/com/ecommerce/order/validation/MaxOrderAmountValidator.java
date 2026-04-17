package com.ecommerce.order.validation;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.exception.InvalidOrderStateException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Concrete Strategy: Prevents orders that exceed a monetary ceiling.
 * Demonstrates how new business rules snap in as independent components.
 */
@Component
public class MaxOrderAmountValidator implements OrderValidationStrategy {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    @Override
    public void validate(CreateOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest item : request.getItems()) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        if (total.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidOrderStateException(
                    String.format("Order total $%s exceeds maximum allowed $%s",
                            total.toPlainString(), MAX_AMOUNT.toPlainString()));
        }
    }
}
