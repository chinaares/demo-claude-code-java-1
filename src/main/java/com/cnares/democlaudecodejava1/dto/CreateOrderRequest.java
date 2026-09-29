package com.cnares.democlaudecodejava1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {
    @NotBlank
    private String customerId;
    @NotEmpty(message = "订单至少包含一个商品")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerId, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.items = items;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
