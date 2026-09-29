package com.cnares.democlaudecodejava1.dto;

import java.util.List;

public class PagedOrderResponse {
    private List<OrderResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PagedOrderResponse() {}

    public PagedOrderResponse(List<OrderResponse> orders, int page, int size, long totalElements) {
        this.orders = orders;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public List<OrderResponse> getOrders() { return orders; }
    public void setOrders(List<OrderResponse> orders) { this.orders = orders; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
}
