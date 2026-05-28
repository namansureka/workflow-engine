package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.StepConfig;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import com.naman.workflow_engine.job.service.WorkflowDefinitionService;
import com.naman.workflow_engine.worker.retry.DeadLetterHandler;
import com.naman.workflow_engine.worker.retry.RetryPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkflowEngine {
    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutorRegistry registry;
    private final RetryPolicy retryPolicy;
    private final DeadLetterHandler deadLetterHandler;
    private final RabbitTemplate rabbitTemplate;

    public void execute(WorkflowExecution execution) throws InterruptedException {

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
                int retryCount=execution.getRetryCount();
                long delay = retryPolicy.delay(retryCount);
                if (retryCount >= stepConfig.getRetryLimit()){
                    deadLetterHandler.handle(execution);
                    return;
                }
                else {
                    execution.setRetryCount(retryCount+1);
                    execution.setStatus(ExecutionStatus.WAITING_RETRY);
                    executionRepository.save(execution);
                    Thread.sleep(delay);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_QUEUE, execution.getId());

                }
                return;
            }
        }
        execution.setStatus(ExecutionStatus.COMPLETED);
        executionRepository.save(execution);
    }
}
