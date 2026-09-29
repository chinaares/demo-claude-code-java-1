# 设计文档：Issue #2 订单系统

**版本**：v1.0  
**日期**：2026-09-29  
**Issue**：[#2 订单系统 - 下单、支付、库存扣减](https://github.com/chinaares/demo-claude-code-java-1/issues/2)  
**状态**：已确认，开始实施

---

## 1. 模块归属与类设计

### 1.1 包结构
```
com.cnares.democlaudecodejava1/
├── model/                              # JPA 实体
│   ├── Order.java                      # 订单聚合根
│   ├── OrderLine.java                  # 订单明细
│   ├── Product.java                    # 商品（含乐观锁）
│   └── Payment.java                    # 支付记录
├── dto/                                # DTO
│   ├── CreateOrderRequest.java         # 下单请求
│   ├── OrderItemRequest.java           # 订单明细请求
│   ├── OrderResponse.java              # 订单响应
│   ├── PaymentRequest.java             # 支付请求
│   └── PaymentResponse.java            # 支付响应
├── repository/                         # 数据访问
│   ├── OrderRepository.java
│   ├── ProductRepository.java
│   └── PaymentRepository.java
├── service/                            # 业务逻辑
│   ├── OrderService.java               # 订单编排
│   ├── InventoryService.java           # 库存管理
│   └── PaymentService.java             # 支付处理
├── controller/                         # REST API
│   ├── OrderController.java            # /api/v1/orders
│   └── PaymentController.java          # /api/v1/payments
└── exception/                          # 异常处理
    └── GlobalExceptionHandler.java
```

### 1.2 类清单

| 类名 | 职责 | 备注 |
|------|------|------|
| `Order` | 订单聚合根，含 OrderLine 列表、状态枚举 | JPA entity |
| `OrderLine` | 订单明细，关联 Product + 单价 | JPA entity |
| `Product` | 商品，含库存数量 + @Version 乐观锁 | JPA entity |
| `Payment` | 支付记录，关联 Order | JPA entity |
| `CreateOrderRequest` | 下单请求 DTO | 验证注解 |
| `OrderItemRequest` | 订单明细请求 DTO | |
| `OrderResponse` | 订单响应 DTO | 不暴露 JPA 实体 |
| `PaymentRequest` | 支付请求 DTO | 含幂等 key |
| `PaymentResponse` | 支付响应 DTO | |
| `OrderRepository` | 订单持久化 + 分页查询 | JpaRepository |
| `ProductRepository` | 商品查询（含乐观锁） | |
| `PaymentRepository` | 支付查询 + 幂等检查 | |
| `OrderService` | 订单生命周期编排 | 核心服务 |
| `InventoryService` | 库存扣减/释放（乐观锁） | 并发安全 |
| `PaymentService` | 支付处理 + 幂等 | |
| `OrderController` | 订单 REST API | |
| `PaymentController` | 支付 REST API | |
| `GlobalExceptionHandler` | 统一异常处理 | 400/404/422/500 |

---

## 2. 行为用例（按实现顺序）

### UC-1：创建订单
**输入**：POST `/api/v1/orders` + CreateOrderRequest

**期望结果**：
- 库存充足 → 201 Created，返回 OrderResponse
- 库存不足 → 422 Unprocessable Entity
- 参数无效 → 400 Bad Request

**业务规则**：
1. 校验商品存在
2. 批量校验库存（所有商品都充足才下单）
3. 乐观锁扣减库存（失败重试 3 次，仍失败返回 422）
4. 创建 Order + OrderLine 记录
5. 事务保证原子性

---

### UC-2：处理支付
**输入**：POST `/api/v1/payments` + PaymentRequest（orderId + paymentKey）

**期望结果**：
- 正常支付 → 200 OK，PaymentResponse
- 重复支付（相同 paymentKey）→ 返回原结果（幂等）
- 订单不存在 → 404 Not Found
- 订单状态非 PENDING → 409 Conflict

**业务规则**：
1. 幂等检查：PaymentRepository.findByPaymentKey() 命中则返回原结果
2. 订单状态校验：只能支付 PENDING 订单
3. 模拟支付网关
4. 更新 Payment + Order 状态

---

### UC-3：支付超时自动取消
**输入**：定时任务（每 5 分钟扫描）

**期望结果**：
- PENDING 订单创建超 30 分钟 → 自动取消，库存释放

**技术实现**：
- @Scheduled 定时任务
- OrderStatus.PENDING + createdAt < 30 分钟前

---

### UC-4：查询订单详情
**输入**：GET `/api/v1/orders/{id}`

**期望结果**：
- 存在 → 200 OK，OrderResponse（含 OrderLine 列表）
- 不存在 → 404 Not Found

---

### UC-5：分页查询订单列表
**输入**：GET `/api/v1/orders?page=0&size=20&status=PENDING`

**期望结果**：
- 返回 Page<OrderResponse>
- 支持 status 筛选（可选）

---

## 3. API 契约

### 3.1 下单
```
POST /api/v1/orders
Content-Type: application/json

Request:
{
  "customerId": "C001",
  "items": [
    { "productId": "P001", "quantity": 2 }
  ]
}

Response 201:
{
  "orderId": 1,
  "status": "PENDING",
  "totalAmount": 199.98,
  "createdAt": "2026-09-29T12:00:00Z",
  "items": [
    { "productId": "P001", "productName": "商品A", "quantity": 2, "unitPrice": 99.99 }
  ]
}

Response 422 (库存不足):
{
  "code": "INSUFFICIENT_STOCK",
  "message": "商品 P001 库存不足，当前库存: 1，需购买: 2",
  "timestamp": "2026-09-29T12:00:00Z"
}
```

### 3.2 支付
```
POST /api/v1/payments
Content-Type: application/json

Request:
{
  "orderId": 1,
  "paymentKey": "pay_123456",
  "amount": 199.98
}

Response 200:
{
  "paymentId": 1,
  "orderId": 1,
  "status": "COMPLETED",
  "paidAt": "2026-09-29T12:05:00Z"
}

Response 409 (订单状态不允许支付):
{
  "code": "ORDER_NOT_PAYABLE",
  "message": "订单状态为 CANCELED，无法支付"
}
```

### 3.3 查询订单
```
GET /api/v1/orders/{id}

Response 200:
{
  "orderId": 1,
  "customerId": "C001",
  "status": "PAID",
  "totalAmount": 199.98,
  "createdAt": "2026-09-29T12:00:00Z",
  "items": [...]
}

GET /api/v1/orders?page=0&size=20&status=PENDING

Response 200:
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

## 4. 风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 超卖（并发库存扣减） | 高 | Product.@Version 乐观锁 + 重试 3 次 |
| 幂等性（重复支付） | 中 | Payment.paymentKey 唯一索引 |
| 库存扣减成功但订单创建失败 | 高 | 本地事务保证原子性 |
| 支付超时未释放库存 | 中 | @Scheduled 每 5 分钟扫描 + 30 分钟超时 |
| 慢查询（订单列表） | 低 | createdAt + status 索引 |

---

## 5. 实现检查清单

- [ ] Order/OrderLine/Product/Payment JPA 实体
- [ ] 乐观锁库存扣减（InventoryService）
- [ ] 支付幂等处理（PaymentService）
- [ ] 订单服务编排（OrderService）
- [ ] REST API 控制器
- [ ] 全局异常处理
- [ ] 定时任务（超时取消）
- [ ] 单元测试（覆盖率 > 80%）
- [ ] data.sql 种子数据（P001, P002）
- [ ] H2 控制台启用（开发调试）

---

## 6. 技术决策（已确认）

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 支付超时时间 | 30 分钟（固定） | issue 默认 |
| 用户认证 | 暂不需要 | 内部 API |
| 用户信息关联 | 暂不需要 | 简化实现 |
| 物流功能 | 不实现 | issue 未提及 |
| 数据库 | H2 内存 | 开发/测试环境 |