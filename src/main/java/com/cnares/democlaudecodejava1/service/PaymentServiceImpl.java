package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.PaymentRequest;
import com.cnares.democlaudecodejava1.dto.PaymentResponse;
import com.cnares.democlaudecodejava1.exception.OrderNotPayableException;
import com.cnares.democlaudecodejava1.exception.ResourceNotFoundException;
import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import com.cnares.democlaudecodejava1.model.Payment;
import com.cnares.democlaudecodejava1.model.PaymentStatus;
import com.cnares.democlaudecodejava1.repository.OrderRepository;
import com.cnares.democlaudecodejava1.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // 幂等检查
        return paymentRepository.findByPaymentKey(request.getPaymentKey())
                .map(existingPayment -> toIdempotentResponse(existingPayment))
                .orElseGet(() -> processNewPayment(request));
    }

    private PaymentResponse processNewPayment(PaymentRequest request) {
        // 校验订单存在
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        // 校验订单状态
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPayableException("Order status is " + order.getStatus() + ", cannot pay");
        }

        // 创建支付记录
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentKey(request.getPaymentKey());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        // 更新订单状态
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        return toResponse(savedPayment);
    }

    private PaymentResponse toIdempotentResponse(Payment payment) {
        PaymentResponse response = toResponse(payment);
        response.setNote("Duplicate request, returning existing result");
        return response;
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setStatus(payment.getStatus());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }
}
