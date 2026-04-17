package com.ecommerce.order.scheduler;

import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderStatusScheduler scheduler;

    @Test
    @DisplayName("should invoke promotePendingOrders on the service")
    void shouldInvokeService() {
        when(orderService.promotePendingOrders()).thenReturn(5);

        scheduler.promotePendingOrders();

        verify(orderService, times(1)).promotePendingOrders();
    }
}
