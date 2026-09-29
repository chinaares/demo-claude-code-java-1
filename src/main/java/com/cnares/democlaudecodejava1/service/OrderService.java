package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.CreateOrderRequest;
import com.cnares.democlaudecodejava1.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
}
