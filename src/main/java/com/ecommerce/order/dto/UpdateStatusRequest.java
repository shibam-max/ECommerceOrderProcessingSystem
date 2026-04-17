package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
