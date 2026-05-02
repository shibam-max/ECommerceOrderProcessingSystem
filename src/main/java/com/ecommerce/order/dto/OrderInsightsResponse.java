package com.ecommerce.order.dto;

import java.math.BigDecimal;

public class OrderInsightsResponse {

    private long totalOrders;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private BigDecimal pendingRevenue;

    public OrderInsightsResponse() {}

    public OrderInsightsResponse(long totalOrders, long pendingOrders, long processingOrders,
                                 long shippedOrders, long deliveredOrders, long cancelledOrders,
                                 BigDecimal totalRevenue, BigDecimal averageOrderValue,
                                 BigDecimal pendingRevenue) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.processingOrders = processingOrders;
        this.shippedOrders = shippedOrders;
        this.deliveredOrders = deliveredOrders;
        this.cancelledOrders = cancelledOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.pendingRevenue = pendingRevenue;
    }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

    public long getProcessingOrders() { return processingOrders; }
    public void setProcessingOrders(long processingOrders) { this.processingOrders = processingOrders; }

    public long getShippedOrders() { return shippedOrders; }
    public void setShippedOrders(long shippedOrders) { this.shippedOrders = shippedOrders; }

    public long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }

    public BigDecimal getPendingRevenue() { return pendingRevenue; }
    public void setPendingRevenue(BigDecimal pendingRevenue) { this.pendingRevenue = pendingRevenue; }
}
