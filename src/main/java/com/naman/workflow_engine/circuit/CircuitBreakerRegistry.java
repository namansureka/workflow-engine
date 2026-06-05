package com.naman.workflow_engine.circuit;

import com.naman.workflow_engine.circuit.fallback.CachedFallback;
import com.naman.workflow_engine.circuit.fallback.DefaultFallback;
import com.naman.workflow_engine.circuit.fallback.DegradedFallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final DefaultFallback defaultFallback;
    private final DegradedFallback degradedFallback;
    private final CachedFallback cachedFallback;

    private final SlidingWindowFailureTracker slidingWindowFailureTracker;

    public CircuitBreaker getBreaker(String serviceId) {

        return breakers.computeIfAbsent(
                serviceId,
                id -> new CircuitBreaker(30000, slidingWindowFailureTracker,
                        serviceId, decideFallback(id))
        );
    }

    private FallbackStrategy decideFallback(String serviceId) {

        return switch (serviceId) {
            case "chargePayment" -> cachedFallback;
            case "sendEmail" -> defaultFallback;
            case "updateInventory" -> degradedFallback;
            default -> defaultFallback;
        };
    }

    public Collection<CircuitBreaker> getAllBreakers() {
        return Collections.unmodifiableCollection(breakers.values());
    }

    public CircuitBreaker findBreaker(String id) {
        CircuitBreaker breaker = breakers.get(id);
        if (breaker == null) {
            throw new RuntimeException("No circuit breaker found: " + id);
        }
        return breaker;
    }
}