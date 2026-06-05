package com.naman.workflow_engine.circuit;

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


    public CircuitBreaker(long cooldownMs,SlidingWindowFailureTracker tracker,String serviceId) {
        this.state = CircuitBreakerState.CLOSED;
        this.cooldownMs = cooldownMs;
        this.slidingWindowFailureTracker= tracker  ;
        this.serviceId=serviceId;
    }

    public void open() {
        state = CircuitBreakerState.OPEN;
        openedTime = Instant.now();
    }

    public void close() {
        state = CircuitBreakerState.CLOSED;
        slidingWindowFailureTracker.resetFailurePressure(serviceId);
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

    public StepResult execute(Supplier<StepResult> action, String serviceId){

        if(!allowRequest()){
            return StepResult.FAILURE;
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
}
