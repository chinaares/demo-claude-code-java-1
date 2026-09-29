package com.cnares.democlaudecodejava1.service;

import com.cnares.democlaudecodejava1.dto.CreateOrderRequest;
import com.cnares.democlaudecodejava1.dto.OrderItemRequest;
import com.cnares.democlaudecodejava1.dto.OrderItemResponse;
import com.cnares.democlaudecodejava1.dto.OrderResponse;
import com.cnares.democlaudecodejava1.dto.PagedOrderResponse;
import com.cnares.democlaudecodejava1.exception.InsufficientStockException;
import com.cnares.democlaudecodejava1.exception.ResourceNotFoundException;
import com.cnares.democlaudecodejava1.model.Order;
import com.cnares.democlaudecodejava1.model.OrderLine;
import com.cnares.democlaudecodejava1.model.OrderStatus;
import com.cnares.democlaudecodejava1.model.Product;
import com.cnares.democlaudecodejava1.repository.OrderRepository;
import com.cnares.democlaudecodejava1.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 校验所有商品存在并扣减库存
        for (OrderItemRequest item : request.getItems()) {
            String productId = item.getProductId();
            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
            if (!inventoryService.reserveStock(productId, item.getQuantity())) {
                throw new InsufficientStockException("Insufficient stock for product: " + productId);
            }
        }

        // 构建订单
        Order order = buildOrder(request);

        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Override
    public PagedOrderResponse getOrders(int page, int size, OrderStatus status) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageRequest);
        } else {
            orderPage = orderRepository.findAll(pageRequest);
        }
        List<OrderResponse> orders = orderPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PagedOrderResponse(orders, page, size, orderPage.getTotalElements());
    }

    private Order buildOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest item : request.getItems()) {
            Product product = productRepository.getById(item.getProductId());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            OrderLine line = new OrderLine(product, item.getQuantity(), product.getPrice());
            line.setOrder(order);
            order.addLine(line);
            totalAmount = totalAmount.add(lineTotal);
        }
        order.setTotalAmount(totalAmount);
        return order;
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(order.getLines().stream()
                .map(line -> new OrderItemResponse(
                        line.getProduct().getProductId(),
                        line.getProduct().getProductName(),
                        line.getQuantity(),
                        line.getUnitPrice()))
                .collect(Collectors.toList()));
        return response;
    }
}
