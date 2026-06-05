package com.naman.workflow_engine.circuit;

import com.naman.workflow_engine.common.dtos.CircuitBreakerResponse;
import com.naman.workflow_engine.worker.StepResult;
import lombok.Getter;

import java.time.Instant;
import java.util.function.Supplier;

@Getter
public class CircuitBreaker {

    private CircuitBreakerState state;
    private Instant openedTime;
    private final long cooldownMs;
    private String serviceId;
    private final SlidingWindowFailureTracker slidingWindowFailureTracker;
    private final FallbackStrategy fallbackStrategy;
    private boolean halfOpenTestSent = false;


    public CircuitBreaker(long cooldownMs, SlidingWindowFailureTracker tracker,
                          String serviceId, FallbackStrategy fallbackStrategy) {
        this.fallbackStrategy = fallbackStrategy;
        this.state = CircuitBreakerState.CLOSED;
        this.cooldownMs = cooldownMs;
        this.slidingWindowFailureTracker= tracker  ;
        this.serviceId=serviceId;
    }

    public void open() {
        state = CircuitBreakerState.OPEN;
        openedTime = Instant.now();
        halfOpenTestSent = false;

    }

    public void close() {
        state = CircuitBreakerState.CLOSED;
        slidingWindowFailureTracker.resetFailurePressure(serviceId);
        halfOpenTestSent = false;
    }

    public void moveToHalfOpen() {
        state = CircuitBreakerState.HALF_OPEN;
    }

    public boolean allowRequest() {
        if (state == CircuitBreakerState.OPEN) {
            if (Instant.now().isAfter(openedTime.plusMillis(cooldownMs))) {
                moveToHalfOpen();
            }
            else{
                return false;
            }
        }
        if(state == CircuitBreakerState.HALF_OPEN) {
            if(!halfOpenTestSent) {
                halfOpenTestSent = true;
                return true;
            }
            return false;
        }
        return true;
    }

    public StepResult execute(Supplier<StepResult> action, String serviceId){

        if(!allowRequest()){
            return fallbackStrategy.getFallback(serviceId);
        }
        StepResult result = action.get();
        if(result==StepResult.SUCCESS){
            slidingWindowFailureTracker.recordSuccess(serviceId);
            if(state == CircuitBreakerState.HALF_OPEN){
                close();
            }
            return StepResult.SUCCESS;
        }

        else {
            slidingWindowFailureTracker.incrementFailurePressure(serviceId);
            slidingWindowFailureTracker.recordFailure(serviceId);
            if(state == CircuitBreakerState.HALF_OPEN){
                open();
                return StepResult.FAILURE;
            }
            if(slidingWindowFailureTracker.getFailureRate(serviceId)>0.50 &&
                    slidingWindowFailureTracker.getFailurePressure(serviceId)>=5){
                open();
            }

            return StepResult.FAILURE;
        }
    }

    public CircuitBreakerResponse toResponse(){
        CircuitBreakerResponse breaker= new CircuitBreakerResponse();
        breaker.setServiceId(serviceId);
        breaker.setState(state);
        if(state == CircuitBreakerState.OPEN) {
            breaker.setOpenedTime(openedTime);
            breaker.setFailurePressure(slidingWindowFailureTracker.getFailurePressure(serviceId));
        }

        return breaker;

    }
}
