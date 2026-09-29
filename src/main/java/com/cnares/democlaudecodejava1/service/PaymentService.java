package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.PaymentRequest;
import com.cnares.democlaudecodejava1.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
}
