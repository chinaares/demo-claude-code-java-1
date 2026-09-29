package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.CreateOrderRequest;
import com.cnares.democlaudecodejava1.dto.OrderResponse;
import com.cnares.democlaudecodejava1.dto.PagedOrderResponse;
import com.cnares.democlaudecodejava1.model.OrderStatus;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrder(Long orderId);
    PagedOrderResponse getOrders(int page, int size, OrderStatus status);
}
