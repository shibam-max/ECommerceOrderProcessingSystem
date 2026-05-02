package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;

import javax.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    public UpdateStatusRequest() {}

    public UpdateStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private OrderStatus status;

        public Builder status(OrderStatus status) { this.status = status; return this; }

        public UpdateStatusRequest build() {
            return new UpdateStatusRequest(status);
        }
    }
}
