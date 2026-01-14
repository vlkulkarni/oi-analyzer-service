package com.oi.market.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.oi.market.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UpstoxAuthException.class)
    public ResponseEntity<ApiResponse<?>> handleUpstoxAuthException(UpstoxAuthException ex) {
        log.error("Upstox auth exception: {}", ex.getMessage());
        ApiResponse<?> response = new ApiResponse<>(false, "Authentication failed: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(WebSocketConnectionException.class)
    public ResponseEntity<ApiResponse<?>> handleWebSocketException(WebSocketConnectionException ex) {
        log.error("WebSocket connection exception: {}", ex.getMessage());
        ApiResponse<?> response = new ApiResponse<>(false, "WebSocket connection error: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ProtobufDecodingException.class)
    public ResponseEntity<ApiResponse<?>> handleProtobufException(ProtobufDecodingException ex) {
        log.error("Protobuf decoding exception: {}", ex.getMessage());
        ApiResponse<?> response = new ApiResponse<>(false, "Data processing error: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        ApiResponse<?> response = new ApiResponse<>(false, "Invalid request: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        ApiResponse<?> response = new ApiResponse<>(false, "Internal server error", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
