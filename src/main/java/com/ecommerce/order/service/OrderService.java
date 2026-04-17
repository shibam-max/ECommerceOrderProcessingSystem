package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderStatusChangedEvent;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.validation.OrderValidationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final List<OrderValidationStrategy> validationStrategies;

    public OrderService(OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher,
                        List<OrderValidationStrategy> validationStrategies) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.validationStrategies = validationStrategies;
    }

    @Transactional
    @CacheEvict(value = "orderList", allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        for (OrderValidationStrategy strategy : validationStrategies) {
            strategy.validate(request);
        }

        Order order = OrderMapper.toEntity(request);
        Order saved = orderRepository.save(order);
        log.info("Created order id={} for customer={}", saved.getId(), saved.getCustomerEmail());

        eventPublisher.publishEvent(new OrderCreatedEvent(
                this, saved.getId(), saved.getCustomerEmail(),
                saved.getItems().size(), saved.getTotalAmount()));

        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(Long id) {
        log.debug("Cache MISS for order id={}", id);
        Order order = findOrderOrThrow(id);
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orderList", key = "#status?.name() ?: 'ALL'")
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
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "orderList", allEntries = true)
    })
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

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                this, id, currentStatus, targetStatus,
                String.format("Status updated from %s to %s", currentStatus, targetStatus)));

        return OrderMapper.toResponse(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "orderList", allEntries = true)
    })
    public OrderResponse cancelOrder(Long id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    String.format("Cannot cancel order %d — only PENDING orders can be cancelled (current: %s)",
                            id, order.getStatus()));
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.save(order);
        log.info("Order id={} cancelled", id);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                this, id, previousStatus, OrderStatus.CANCELLED,
                "Order cancelled by customer"));

        return OrderMapper.toResponse(cancelled);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderList", allEntries = true)
    })
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
