package com.oi.market.exception;

public class UpstoxAuthException extends RuntimeException {
    
    public UpstoxAuthException(String message) {
        super(message);
    }
    
    public UpstoxAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
