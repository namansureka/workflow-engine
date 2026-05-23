package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.job.model.WorkflowExecution;

public interface StepExecutor {
    StepResult execute(WorkflowExecution execution);
}