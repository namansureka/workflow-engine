package com.naman.workflow_engine.circuit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    private final SlidingWindowFailureTracker slidingWindowFailureTracker;
    public CircuitBreaker getBreaker(String serviceId) {

        return breakers.computeIfAbsent(
                serviceId,
                id -> new CircuitBreaker(30000,slidingWindowFailureTracker,serviceId)
        );
    }
}