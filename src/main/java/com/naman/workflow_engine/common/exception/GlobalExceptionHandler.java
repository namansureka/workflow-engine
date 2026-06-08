package com.naman.workflow_engine.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        Map<String, Object> body = Map.of("status", 404, "error", ex.getMessage());
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(CircuitOpenException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(CircuitOpenException ex) {
        Map<String, Object> body = Map.of("status", 503, "error", ex.getMessage());
        return ResponseEntity.status(503).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = Map.of("status", 400, "error", ex.getMessage());
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> body = Map.of("status", 500, "error", ex.getMessage());
        return ResponseEntity.status(500).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = Map.of("status", 500, "error", "Internal server error");
        return ResponseEntity.status(500).body(body);
    }
}