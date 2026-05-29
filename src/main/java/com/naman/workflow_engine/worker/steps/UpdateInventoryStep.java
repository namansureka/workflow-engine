package com.naman.workflow_engine.worker.steps;

import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.worker.StepExecutor;
import com.naman.workflow_engine.worker.StepResult;
import org.springframework.stereotype.Component;

@Component
public class UpdateInventoryStep implements StepExecutor {

    @Override
    public StepResult execute(WorkflowExecution execution)
    {
        System.out.println("Updating Inventory for: " + execution.getWorkflowName());

        if (Math.random() < 0.3) {
            System.out.println("Updating FAILED");
            return StepResult.FAILURE;
        }

        System.out.println("Updating SUCCESS");
        return StepResult.SUCCESS;
    }

    @Override
    public String getStepName() { return "update-inventory"; }

}
