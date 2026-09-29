package com.cnares.democlaudecodejava1.exception;

public class OrderNotPayableException extends RuntimeException {
    public OrderNotPayableException(String message) {
        super(message);
    }
}
