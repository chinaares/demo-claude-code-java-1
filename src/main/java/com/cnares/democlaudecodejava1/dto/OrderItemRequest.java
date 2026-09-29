package com.cnares.democlaudecodejava1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class OrderItemRequest {
    @NotBlank
    private String productId;
    @Min(1)
    private Integer quantity;

    public OrderItemRequest() {}

    public OrderItemRequest(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
