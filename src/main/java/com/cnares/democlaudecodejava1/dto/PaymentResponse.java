package com.cnares.democlaudecodejava1.dto;

import com.cnares.democlaudecodejava1.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private PaymentStatus status;
    private Instant paidAt;
    private String note;

    public PaymentResponse() {}

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
