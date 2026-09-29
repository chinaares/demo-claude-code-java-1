package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.CreateOrderRequest;
import com.cnares.democlaudecodejava1.dto.OrderItemRequest;
import com.cnares.democlaudecodejava1.dto.OrderResponse;
import com.cnares.democlaudecodejava1.exception.InsufficientStockException;
import com.cnares.democlaudecodejava1.exception.ResourceNotFoundException;
import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import com.cnares.democlaudecodejava1.model.Product;
import com.cnares.democlaudecodejava1.repository.OrderRepository;
import com.cnares.democlaudecodejava1.repository.ProductRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - UC-1: 创建订单")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequest createOrderRequest;
    private OrderItemRequest orderItemRequest;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderItemRequest = new OrderItemRequest("P001", 2);
        createOrderRequest = new CreateOrderRequest("C001", List.of(orderItemRequest));

        testProduct = new Product("P001", "Test Product", BigDecimal.valueOf(99.99), 100);
        testProduct.setVersion(0L);
    }

    private Product createProduct(String productId, String name, BigDecimal price, int stock) {
        Product p = new Product(productId, name, price, stock);
        p.setVersion(0L);
        return p;
    }

    @Nested
    @DisplayName("创建订单 - 库存充足")
    class WhenStockSufficient {

        @Test
        @DisplayName("应返回 PENDING 状态的订单响应")
        void shouldReturnPendingOrderResponse_whenStockIsSufficient() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(productRepository.getById("P001")).thenReturn(testProduct);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(Instant.now());
                return order;
            });

            // Act
            OrderResponse response = orderService.createOrder(createOrderRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getCustomerId()).isEqualTo("C001");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductId()).isEqualTo("P001");
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("应扣减库存")
        void shouldReserveStock_whenCreatingOrder() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(productRepository.getById("P001")).thenReturn(testProduct);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(Instant.now());
                return order;
            });

            // Act
            orderService.createOrder(createOrderRequest);

            // Assert
            verify(inventoryService).reserveStock("P001", 2);
        }

        @Test
        @DisplayName("应保存订单")
        void shouldSaveOrder_whenCreatingOrder() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(productRepository.getById("P001")).thenReturn(testProduct);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(Instant.now());
                return order;
            });

            // Act
            orderService.createOrder(createOrderRequest);

            // Assert
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("创建订单 - 库存不足")
    class WhenStockInsufficient {

        @Test
        @DisplayName("应抛出库存不足异常")
        void shouldThrowInsufficientStockException_whenStockIsInsufficient() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("P001");
        }

        @Test
        @DisplayName("不应保存订单")
        void shouldNotSaveOrder_whenStockIsInsufficient() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(false);

            // Act
            try {
                orderService.createOrder(createOrderRequest);
            } catch (InsufficientStockException ignored) {
            }

            // Assert
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("不应扣减库存")
        void shouldNotReserveStock_whenStockIsInsufficient() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(true);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(false);

            // Act
            try {
                orderService.createOrder(createOrderRequest);
            } catch (InsufficientStockException ignored) {
            }

            // Assert
            verify(inventoryService, never()).releaseStock(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("创建订单 - 商品不存在")
    class WhenProductNotFound {

        @Test
        @DisplayName("应抛出资源不存在异常")
        void shouldThrowResourceNotFoundException_whenProductNotFound() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("P001");
        }

        @Test
        @DisplayName("不应尝试扣减库存")
        void shouldNotReserveStock_whenProductNotFound() {
            // Arrange
            when(productRepository.existsById("P001")).thenReturn(false);

            // Act
            try {
                orderService.createOrder(createOrderRequest);
            } catch (ResourceNotFoundException ignored) {
            }

            // Assert
            verify(inventoryService, never()).reserveStock(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("创建订单 - 多商品场景")
    class WhenMultipleItems {

        @Test
        @DisplayName("应创建包含多个商品的订单")
        void shouldCreateOrderWithMultipleItems() {
            // Arrange
            OrderItemRequest item2 = new OrderItemRequest("P002", 1);
            CreateOrderRequest multiItemRequest = new CreateOrderRequest("C001", List.of(orderItemRequest, item2));
            Product testProduct2 = createProduct("P002", "Test Product 2", BigDecimal.valueOf(49.99), 50);

            when(productRepository.existsById("P001")).thenReturn(true);
            when(productRepository.existsById("P002")).thenReturn(true);
            when(productRepository.getById("P001")).thenReturn(testProduct);
            when(productRepository.getById("P002")).thenReturn(testProduct2);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(true);
            when(inventoryService.reserveStock("P002", 1)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(Instant.now());
                return order;
            });

            // Act
            OrderResponse response = orderService.createOrder(multiItemRequest);

            // Assert
            assertThat(response.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("任一商品库存不足应整体失败")
        void shouldFailEntirely_whenAnyItemHasInsufficientStock() {
            // Arrange
            OrderItemRequest item2 = new OrderItemRequest("P002", 1);
            CreateOrderRequest multiItemRequest = new CreateOrderRequest("C001", List.of(orderItemRequest, item2));

            when(productRepository.existsById("P001")).thenReturn(true);
            when(productRepository.existsById("P002")).thenReturn(true);
            when(inventoryService.reserveStock("P001", 2)).thenReturn(true);
            when(inventoryService.reserveStock("P002", 1)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(multiItemRequest))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }
}
