package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.StepConfig;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import com.naman.workflow_engine.job.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkflowEngine {
    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutorRegistry registry;

    public void execute(WorkflowExecution execution) {

        WorkflowDefinition definition=definitionService.getDefinition(execution.getWorkflowName());
        List<StepConfig> steps = definition.getSteps();
        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        int startIndex = 0;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStepName().equals(execution.getCurrentStep())) {
                startIndex = i;
                break;
            }
        }

        for (int i = startIndex; i < steps.size(); i++) {
            StepConfig stepConfig = steps.get(i);
            StepExecutor executor = registry.getExecutor(stepConfig.getStepName());

            StepResult result = executor.execute(execution);

            if (result == StepResult.SUCCESS) {
                if (i + 1 < steps.size()) {
                    execution.setCurrentStep(steps.get(i + 1).getStepName());
                }
                executionRepository.save(execution);
            } else {
                // FAILURE — handle retry later
                return;
            }
        }
        execution.setStatus(ExecutionStatus.COMPLETED);
        executionRepository.save(execution);
    }
}
