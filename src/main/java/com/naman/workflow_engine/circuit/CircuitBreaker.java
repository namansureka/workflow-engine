package com.naman.workflow_engine.circuit;

import lombok.Getter;

import java.time.Instant;

@Getter
public class CircuitBreaker {

    private CircuitBreakerState state;
    private Instant openedTime;
    private final long cooldownMs;

    public CircuitBreaker(long cooldownMs) {
        this.state = CircuitBreakerState.CLOSED;
        this.cooldownMs = cooldownMs;
    }

    public void open() {
        state = CircuitBreakerState.OPEN;
        openedTime = Instant.now();
    }

    public void close() {
        state = CircuitBreakerState.CLOSED;
    }

    public void moveToHalfOpen() {
        state = CircuitBreakerState.HALF_OPEN;
    }

    public boolean allowRequest() {
        if (state == CircuitBreakerState.OPEN) {
            if (Instant.now().isAfter(openedTime.plusMillis(cooldownMs))) {
                moveToHalfOpen();
                return true;
            }
            return false;
        }
        return true;
    }
}
