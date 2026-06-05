package com.naman.workflow_engine.circuit.controller;

import com.naman.workflow_engine.circuit.CircuitBreaker;
import com.naman.workflow_engine.circuit.CircuitBreakerRegistry;
import com.naman.workflow_engine.common.dtos.CircuitBreakerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/circuit-breakers")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry registry;

    @GetMapping("/status")
    public List<CircuitBreakerResponse> getStatus() {
        return registry.getAllBreakers().stream()
                .map(breaker -> breaker.toResponse())
                .toList();
    }

    @PostMapping("/reset/{id}")
    public String resetBreaker(@PathVariable String id) {
        CircuitBreaker breaker = registry.findBreaker(id);
        breaker.close();
        return "Circuit breaker reset successfully for service: " + id;
    }
}
