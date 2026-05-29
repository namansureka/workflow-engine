package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.StepConfig;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import com.naman.workflow_engine.job.service.WorkflowDefinitionService;
import com.naman.workflow_engine.worker.idempotency.IdempotencyService;
import com.naman.workflow_engine.worker.retry.DeadLetterHandler;
import com.naman.workflow_engine.worker.retry.RetryPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {
    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutorRegistry registry;
    private final RetryPolicy retryPolicy;
    private final DeadLetterHandler deadLetterHandler;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;

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

            if(idempotencyService.isAlreadyExecuted(execution.getId(), stepConfig.getStepName())){
                log.info("Step already executed, skipping: {} for execution: {}", stepConfig.getStepName(), execution.getId());

                continue;

            }

            log.info("Executing step: {} for execution: {}", stepConfig.getStepName(), execution.getId());

            StepResult result = executor.execute(execution);

            if (result == StepResult.SUCCESS) {
                if (i + 1 < steps.size()) {
                    execution.setCurrentStep(steps.get(i + 1).getStepName());
                }
                idempotencyService.markAsExecuted(execution.getId(), stepConfig.getStepName());
                executionRepository.save(execution);
                log.info("Step SUCCESS: {} for execution: {}", stepConfig.getStepName(), execution.getId());

            } else {
                int retryCount=execution.getRetryCount();
                if (retryCount >= stepConfig.getRetryLimit()){
                    log.error("Step permanently FAILED: {} for execution: {}, sending to DLQ", stepConfig.getStepName(), execution.getId());

                    deadLetterHandler.handle(execution);
                    return;
                }
                else {
                    long delay = retryPolicy.delay(retryCount);
                    log.warn("Step FAILED: {} for execution: {}, retry count: {}, retrying after {}ms",
                            stepConfig.getStepName(), execution.getId(), retryCount, delay);
                    execution.setRetryCount(retryCount+1);
                    execution.setStatus(ExecutionStatus.WAITING_RETRY);
                    executionRepository.save(execution);
                    Thread.sleep(delay);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_QUEUE, execution.getId());

                }
                return;
            }
        }
        log.info("Workflow COMPLETED for execution: {}", execution.getId());

        execution.setStatus(ExecutionStatus.COMPLETED);
        executionRepository.save(execution);
    }
}
