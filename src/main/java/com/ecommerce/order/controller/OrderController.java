package com.ecommerce.order.controller;

import com.ecommerce.order.audit.OrderAuditRepository;
import com.ecommerce.order.audit.OrderAuditResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateStatusRequest;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "E-commerce order management endpoints")
public class OrderController {

    private final OrderService orderService;
    private final OrderAuditRepository auditRepository;

    public OrderController(OrderService orderService, OrderAuditRepository auditRepository) {
        this.orderService = orderService;
        this.auditRepository = auditRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Places a new order with one or more items")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves full order details including items")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping
    @Operation(summary = "List all orders (paginated)",
               description = "Returns orders with pagination. Optionally filtered by status. " +
                       "Defaults to page=0, size=20, sorted by createdAt descending.")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @Parameter(description = "Filter by order status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (createdAt, totalAmount, customerName)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {
        int clampedSize = Math.min(size, 100);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, clampedSize, sort);
        return ResponseEntity.ok(orderService.listOrdersPaged(status, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Transitions order to a new status (validates allowed transitions)")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels a PENDING order — returns 409 if order is not in PENDING status")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Get order audit trail", description = "Returns the full audit history for an order — every status change, timestamps, and details")
    public ResponseEntity<List<OrderAuditResponse>> getOrderAudit(@PathVariable Long id) {
        orderService.getOrder(id);
        List<OrderAuditResponse> responses = new ArrayList<>();
        new ArrayList<>(auditRepository.findByOrderIdOrderByOccurredAtAsc(id))
                .forEach(log -> responses.add(OrderAuditResponse.fromEntity(log)));
        return ResponseEntity.ok(responses);
    }
}
