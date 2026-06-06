package com.naman.workflow_engine.dashboard;

import com.naman.workflow_engine.job.model.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutionStatusEvent {
    private long executionId;

    private String workflowName;

    private String currentStep;

    private ExecutionStatus status;

    private LocalDateTime timestamp;
}
