package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = OrderMapper.toEntity(request);
        Order saved = orderRepository.save(order);
        log.info("Created order id={} for customer={}", saved.getId(), saved.getCustomerEmail());
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = findOrderOrThrow(id);
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus status) {
        List<Order> orders = (status != null)
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();

        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(OrderMapper.toResponse(order));
        }
        return responses;
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateStatusRequest request) {
        Order order = findOrderOrThrow(id);
        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(
                    String.format("Cannot transition order %d from %s to %s",
                            id, currentStatus, targetStatus));
        }

        order.setStatus(targetStatus);
        Order updated = orderRepository.save(order);
        log.info("Order id={} status changed {} -> {}", id, currentStatus, targetStatus);
        return OrderMapper.toResponse(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    String.format("Cannot cancel order %d — only PENDING orders can be cancelled (current: %s)",
                            id, order.getStatus()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.save(order);
        log.info("Order id={} cancelled", id);
        return OrderMapper.toResponse(cancelled);
    }

    @Transactional
    public int promotePendingOrders() {
        int count = orderRepository.bulkUpdatePendingToProcessing();
        if (count > 0) {
            log.info("Promoted {} PENDING order(s) to PROCESSING", count);
        }
        return count;
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
