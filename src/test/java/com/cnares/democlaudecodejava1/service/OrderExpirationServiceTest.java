package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderLine;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import com.cnares.democlaudecodejava1.model.Product;
import com.cnares.democlaudecodejava1.repository.OrderRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExpirationService - UC-3: 支付超时自动取消")
class OrderExpirationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderExpirationServiceImpl orderExpirationService;

    private Order expiredOrderWithLine;
    private Order validOrder;

    @BeforeEach
    void setUp() {
        // 创建商品
        Product product1 = new Product("P001", "Test Product", BigDecimal.valueOf(99.99), 10);
        Product product2 = new Product("P002", "Test Product 2", BigDecimal.valueOf(49.99), 5);

        // 创建订单行
        OrderLine line1 = new OrderLine(product1, 2, BigDecimal.valueOf(99.99));
        OrderLine line2 = new OrderLine(product2, 1, BigDecimal.valueOf(49.99));

        // 已超时 31 分钟的订单（含订单行）
        expiredOrderWithLine = new Order();
        expiredOrderWithLine.setId(1L);
        expiredOrderWithLine.setCustomerId("C001");
        expiredOrderWithLine.setStatus(OrderStatus.PENDING);
        expiredOrderWithLine.setTotalAmount(BigDecimal.valueOf(199.98));
        expiredOrderWithLine.setCreatedAt(Instant.now().minus(31, ChronoUnit.MINUTES));
        expiredOrderWithLine.getLines().add(line1);
        expiredOrderWithLine.getLines().add(line2);
        line1.setOrder(expiredOrderWithLine);
        line2.setOrder(expiredOrderWithLine);

        // 未超时的订单
        validOrder = new Order();
        validOrder.setId(2L);
        validOrder.setCustomerId("C002");
        validOrder.setStatus(OrderStatus.PENDING);
        validOrder.setTotalAmount(BigDecimal.valueOf(99.99));
        validOrder.setCreatedAt(Instant.now().minus(15, ChronoUnit.MINUTES));
    }

    @Nested
    @DisplayName("超时订单处理")
    class WhenCancelingExpiredOrders {

        @Test
        @DisplayName("应取消超过30分钟的待支付订单")
        void shouldCancelOrdersOlderThan30Minutes() {
            // Arrange
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Arrays.asList(expiredOrderWithLine, validOrder));

            // Act
            orderExpirationService.cancelExpiredOrders();

            // Assert
            verify(orderRepository).save(argThat(order ->
                    order.getId().equals(1L) && order.getStatus() == OrderStatus.CANCELED));
        }

        @Test
        @DisplayName("应释放超时订单的库存")
        void shouldReleaseStockForExpiredOrders() {
            // Arrange
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Collections.singletonList(expiredOrderWithLine));

            // Act
            orderExpirationService.cancelExpiredOrders();

            // Assert
            verify(inventoryService).releaseStock("P001", 2);
            verify(inventoryService).releaseStock("P002", 1);
        }

        @Test
        @DisplayName("不应修改未超时订单的状态")
        void shouldNotModifyValidOrders() {
            // Arrange
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Arrays.asList(expiredOrderWithLine, validOrder));

            // Act
            orderExpirationService.cancelExpiredOrders();

            // Assert
            verify(orderRepository, never()).save(argThat(order ->
                    order.getId().equals(2L) && order.getStatus() == OrderStatus.CANCELED));
        }
    }

    @Nested
    @DisplayName("无超时订单场景")
    class WhenNoExpiredOrders {

        @Test
        @DisplayName("没有超时订单时应不操作")
        void shouldDoNothing_whenNoExpiredOrders() {
            // Arrange
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Collections.singletonList(validOrder));

            // Act
            orderExpirationService.cancelExpiredOrders();

            // Assert
            verify(orderRepository, never()).save(any(Order.class));
            verify(inventoryService, never()).releaseStock(anyString(), anyInt());
        }

        @Test
        @DisplayName("没有待支付订单时应不操作")
        void shouldDoNothing_whenNoPendingOrders() {
            // Arrange
            when(orderRepository.findByStatus(OrderStatus.PENDING))
                    .thenReturn(Collections.emptyList());

            // Act
            orderExpirationService.cancelExpiredOrders();

            // Assert
            verify(orderRepository, never()).save(any(Order.class));
            verify(inventoryService, never()).releaseStock(anyString(), anyInt());
        }
    }
}
