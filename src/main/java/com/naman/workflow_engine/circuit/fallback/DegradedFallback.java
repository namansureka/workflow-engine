package com.naman.workflow_engine.circuit.fallback;

import com.naman.workflow_engine.circuit.FallbackStrategy;
import com.naman.workflow_engine.worker.StepResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DegradedFallback implements FallbackStrategy {

    @Override
    public StepResult getFallback(String serviceId) {
        log.warn("Degraded fallback triggered for service: {}." +
                " Returning partial data — downstream results may be incomplete.", serviceId);

        return StepResult.SUCCESS;
    }
}
