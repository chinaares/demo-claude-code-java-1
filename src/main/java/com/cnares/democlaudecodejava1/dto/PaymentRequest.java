package com.cnares.democlaudecodejava1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {
    @NotNull
    private Long orderId;
    @NotBlank
    private String paymentKey;
    @NotNull
    private BigDecimal amount;

    public PaymentRequest() {}

    public PaymentRequest(Long orderId, String paymentKey, BigDecimal amount) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentKey() { return paymentKey; }
    public void setPaymentKey(String paymentKey) { this.paymentKey = paymentKey; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
