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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - UC-2: 处理支付")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order testOrder;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setCustomerId("C001");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(199.98));
        testOrder.setCreatedAt(Instant.now());

        paymentRequest = new PaymentRequest(1L, "pay_123456", BigDecimal.valueOf(199.98));
    }

    @Nested
    @DisplayName("正常支付流程")
    class WhenPaymentSucceeds {

        @Test
        @DisplayName("应返回 COMPLETED 状态的支付响应")
        void shouldReturnCompletedPaymentResponse_whenPaymentSucceeds() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(paymentRepository.findByPaymentKey("pay_123456")).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setId(1L);
                p.setPaidAt(Instant.now());
                return p;
            });

            // Act
            PaymentResponse response = paymentService.processPayment(paymentRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPaymentId()).isEqualTo(1L);
            assertThat(response.getOrderId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(response.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("应更新订单状态为 PAID")
        void shouldUpdateOrderStatusToPaid_whenPaymentSucceeds() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(paymentRepository.findByPaymentKey("pay_123456")).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setId(1L);
                p.setPaidAt(Instant.now());
                return p;
            });

            // Act
            paymentService.processPayment(paymentRequest);

            // Assert
            verify(orderRepository).save(argThat(order -> order.getStatus() == OrderStatus.PAID));
        }
    }

    @Nested
    @DisplayName("幂等性处理")
    class WhenDuplicatePayment {

        @Test
        @DisplayName("重复支付应返回原结果，不重复扣款")
        void shouldReturnExistingResult_whenPaymentKeyExists() {
            // Arrange
            Payment existingPayment = new Payment();
            existingPayment.setId(1L);
            existingPayment.setOrder(testOrder);
            existingPayment.setPaymentKey("pay_123456");
            existingPayment.setAmount(BigDecimal.valueOf(199.98));
            existingPayment.setStatus(PaymentStatus.COMPLETED);
            existingPayment.setPaidAt(Instant.now().minusSeconds(300));

            when(paymentRepository.findByPaymentKey("pay_123456")).thenReturn(Optional.of(existingPayment));

            // Act
            PaymentResponse response = paymentService.processPayment(paymentRequest);

            // Assert
            assertThat(response.getPaymentId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(response.getNote()).isEqualTo("Duplicate request, returning existing result");

            // 验证没有创建新支付
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("订单不存在")
    class WhenOrderNotFound {

        @Test
        @DisplayName("应抛出资源不存在异常")
        void shouldThrowResourceNotFoundException_whenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processPayment(paymentRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("订单状态不允许支付")
    class WhenOrderNotPayable {

        @Test
        @DisplayName("订单已取消时应抛出异常")
        void shouldThrowOrderNotPayableException_whenOrderCanceled() {
            // Arrange
            testOrder.setStatus(OrderStatus.CANCELED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processPayment(paymentRequest))
                    .isInstanceOf(OrderNotPayableException.class)
                    .hasMessageContaining("CANCELED");
        }

        @Test
        @DisplayName("订单已支付时应抛出异常")
        void shouldThrowOrderNotPayableException_whenOrderAlreadyPaid() {
            // Arrange
            testOrder.setStatus(OrderStatus.PAID);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processPayment(paymentRequest))
                    .isInstanceOf(OrderNotPayableException.class)
                    .hasMessageContaining("PAID");
        }
    }
}
