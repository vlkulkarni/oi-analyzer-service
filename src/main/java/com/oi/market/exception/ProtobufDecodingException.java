package com.oi.market.exception;

public class ProtobufDecodingException extends RuntimeException {
    
    public ProtobufDecodingException(String message) {
        super(message);
    }
    
    public ProtobufDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
