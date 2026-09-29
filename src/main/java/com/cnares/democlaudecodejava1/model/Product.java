package com.cnares.democlaudecodejava1.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
public class Product {
    @Id
    private String productId;
    private String productName;
    private BigDecimal price;
    private Integer stockQuantity;
    @Version
    private Long version;

    public Product() {}

    public Product(String productId, String productName, BigDecimal price, Integer stockQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
