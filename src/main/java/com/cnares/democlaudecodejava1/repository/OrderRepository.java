package com.cnares.democlaudecodejava1.repository;

import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
}
