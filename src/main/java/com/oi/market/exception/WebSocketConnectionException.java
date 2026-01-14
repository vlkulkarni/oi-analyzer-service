package com.oi.market.exception;

public class WebSocketConnectionException extends RuntimeException {
    
    public WebSocketConnectionException(String message) {
        super(message);
    }
    
    public WebSocketConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
