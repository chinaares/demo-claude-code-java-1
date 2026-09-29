package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import com.cnares.democlaudecodejava1.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OrderExpirationServiceImpl implements OrderExpirationService {

    private static final int EXPIRATION_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderExpirationServiceImpl(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        for (Order order : pendingOrders) {
            if (isExpired(order)) {
                cancelOrder(order);
            }
        }
    }

    private boolean isExpired(Order order) {
        Instant expirationTime = Instant.now().minus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);
        return order.getCreatedAt().isBefore(expirationTime);
    }

    private void cancelOrder(Order order) {
        // 释放库存
        order.getLines().forEach(line -> {
            inventoryService.releaseStock(line.getProduct().getProductId(), line.getQuantity());
        });

        // 取消订单
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }
}
