package com.naman.workflow_engine.circuit;

import com.naman.workflow_engine.worker.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CircuitBreakerTest {

    @Mock
    private SlidingWindowFailureTracker tracker;

    @Mock
    private FallbackStrategy fallbackStrategy;

    private CircuitBreaker circuitBreaker;
    private static final String SERVICE_ID = "testService";
    private static final long COOLDOWN_MS = 1000;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(COOLDOWN_MS, tracker, SERVICE_ID, fallbackStrategy);
    }

    @Test
    void shouldStartInClosedState() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldAllowRequestWhenClosed() {
        Supplier<StepResult> successAction = () -> StepResult.SUCCESS;

        StepResult result = circuitBreaker.execute(successAction, SERVICE_ID);

        assertSame(StepResult.SUCCESS, result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldOpenAfterFailureThreshold() {
        // Mock tracker to return high failure rate and pressure
        when(tracker.getFailureRate(SERVICE_ID)).thenReturn(0.6);
        when(tracker.getFailurePressure(SERVICE_ID)).thenReturn(5);

        Supplier<StepResult> failingAction = () -> StepResult.FAILURE;

        StepResult result = circuitBreaker.execute(failingAction, SERVICE_ID);

        assertSame(StepResult.FAILURE, result);
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }

    @Test
    void shouldMoveToHalfOpenAfterCooldown() throws InterruptedException {
        // Open the circuit
        circuitBreaker.open();
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());

        // Wait for cooldown to expire (cooldownMs = 1000)
        Thread.sleep(1100);

        // Call allowRequest() which should move state to HALF_OPEN
        circuitBreaker.allowRequest();

        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    void shouldReturnFallbackWhenOpen() {
        // Open the circuit
        circuitBreaker.open();
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());

        // Mock fallback strategy to return SUCCESS
        when(fallbackStrategy.getFallback(SERVICE_ID)).thenReturn(StepResult.SUCCESS);

        // Execute with failing action
        Supplier<StepResult> failingAction = () -> StepResult.FAILURE;
        StepResult result = circuitBreaker.execute(failingAction, SERVICE_ID);

        // Assert fallback was returned
        assertSame(StepResult.SUCCESS, result);
    }

    @Test
    void shouldCloseAfterSuccessfulHalfOpenRequest() {
        // Move to HALF_OPEN state
        circuitBreaker.moveToHalfOpen();
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());

        // Execute with successful action
        Supplier<StepResult> successAction = () -> StepResult.SUCCESS;
        StepResult result = circuitBreaker.execute(successAction, SERVICE_ID);

        // Assert state becomes CLOSED
        assertSame(StepResult.SUCCESS, result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldReopenAfterFailedHalfOpenRequest() {
        // Move to HALF_OPEN state
        circuitBreaker.moveToHalfOpen();
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());

        // Execute with failing action
        Supplier<StepResult> failingAction = () -> StepResult.FAILURE;
        StepResult result = circuitBreaker.execute(failingAction, SERVICE_ID);

        // Assert state becomes OPEN
        assertSame(StepResult.FAILURE, result);
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }

    @Test
    void shouldBlockSecondRequestInHalfOpen() {
        // Move to HALF_OPEN state
        circuitBreaker.moveToHalfOpen();
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());

        // First allowRequest() should return true
        boolean firstRequest = circuitBreaker.allowRequest();
        assertEquals(true, firstRequest);

        // Second allowRequest() should return false
        boolean secondRequest = circuitBreaker.allowRequest();
        assertEquals(false, secondRequest);
    }
}
