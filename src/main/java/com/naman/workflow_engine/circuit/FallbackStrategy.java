package com.naman.workflow_engine.circuit;

import com.naman.workflow_engine.worker.StepResult;

public interface FallbackStrategy {

    StepResult getFallback(String serviceId);
}
