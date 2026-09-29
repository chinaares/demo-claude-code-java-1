package com.cnares.democlaudecodejava1.service;

public interface InventoryService {
    boolean reserveStock(String productId, Integer quantity);
    void releaseStock(String productId, Integer quantity);
}
