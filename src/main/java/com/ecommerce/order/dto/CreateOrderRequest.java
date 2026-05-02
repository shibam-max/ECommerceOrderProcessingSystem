package com.ecommerce.order.dto;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Must be a valid email address")
    private String customerEmail;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerName, String customerEmail, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.items = items;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String customerName;
        private String customerEmail;
        private List<OrderItemRequest> items;

        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder customerEmail(String customerEmail) { this.customerEmail = customerEmail; return this; }
        public Builder items(List<OrderItemRequest> items) { this.items = items; return this; }

        public CreateOrderRequest build() {
            return new CreateOrderRequest(customerName, customerEmail, items);
        }
    }
}
