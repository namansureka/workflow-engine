package com.naman.workflow_engine.circuit.fallback;

import com.naman.workflow_engine.circuit.FallbackStrategy;
import com.naman.workflow_engine.worker.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultFallback implements FallbackStrategy {

    @Override
    public StepResult getFallback(String serviceId) {
        log.warn("Executing default fallback strategy." +
                "Service is currently unavailable. Service ID: {}", serviceId);

        return StepResult.SUCCESS;
    }
}

