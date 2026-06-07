package com.naman.workflow_engine.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StepMetricsDTO {
    String stepName;
    long totalExecutions;
    long successCount;
    long failureCount;
    double successRate;
    double avgDurationMs;
}
