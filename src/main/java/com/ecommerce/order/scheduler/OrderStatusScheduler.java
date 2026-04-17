package com.ecommerce.order.scheduler;

import com.ecommerce.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that promotes all PENDING orders to PROCESSING every 5 minutes.
 */
@Component
public class OrderStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusScheduler.class);

    private final OrderService orderService;

    public OrderStatusScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "${order.scheduler.pending-to-processing-cron}")
    public void promotePendingOrders() {
        log.debug("Running scheduled promotion of PENDING orders to PROCESSING");
        int promoted = orderService.promotePendingOrders();
        log.debug("Scheduled job complete — promoted {} order(s)", promoted);
    }
}
