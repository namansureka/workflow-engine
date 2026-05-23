package com.naman.workflow_engine.job.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StepConfig {
    private String stepName;
    private int retryLimit;
    private long timeoutMs;

}
