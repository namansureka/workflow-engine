package com.naman.workflow_engine.circuit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker getBreaker(String serviceId) {

        return breakers.computeIfAbsent(
                serviceId,
                id -> new CircuitBreaker(30000)
        );
    }
}