package com.ecommerce.order.dto;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility for converting between JPA entities and API DTOs.
 */
public final class OrderMapper {

    private OrderMapper() {}

    public static Order toEntity(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build();
            order.addItem(item);
        }

        order.recalculateTotal();
        return order;
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemResponses.add(toItemResponse(item));
        }

        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
