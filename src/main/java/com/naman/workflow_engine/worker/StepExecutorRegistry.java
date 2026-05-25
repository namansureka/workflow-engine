package com.naman.workflow_engine.worker;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StepExecutorRegistry {
    private final Map<String, StepExecutor> registry = new HashMap<>();

    public StepExecutorRegistry(List<StepExecutor> executors) {
        for (StepExecutor executor : executors) {
            registry.put(executor.getStepName(), executor);
        }
    }

    public StepExecutor getExecutor(String stepName) {
        return registry.get(stepName);
    }
}


