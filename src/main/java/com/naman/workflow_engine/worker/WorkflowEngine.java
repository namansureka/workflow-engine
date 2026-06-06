package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.circuit.CircuitBreaker;
import com.naman.workflow_engine.circuit.CircuitBreakerRegistry;
import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.dashboard.ExecutionStatusEvent;
import com.naman.workflow_engine.dashboard.WebSocketEventPublisher;
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

import java.time.Instant;
import java.time.LocalDateTime;
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
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final WebSocketEventPublisher publisher;

    public void execute(WorkflowExecution execution) {

        WorkflowDefinition definition=definitionService.getDefinition(execution.getWorkflowName());
        List<StepConfig> steps = definition.getSteps();
        execution.setStatus(ExecutionStatus.RUNNING);
        saveAndPublish(execution);

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

            // Handle missing executor
            if (executor == null) {
                log.error("Step executor not found: {} for execution: {}", stepConfig.getStepName(), execution.getId());
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setFailureReason("Step executor not found: " + stepConfig.getStepName());
                saveAndPublish(execution);
                deadLetterHandler.handle(execution);
                return;
            }

            if(idempotencyService.isAlreadyExecuted(execution.getId(), stepConfig.getStepName())){
                log.info("Step already executed, skipping: {} for execution: {}", stepConfig.getStepName(), execution.getId());
                continue;

            }

            log.info("Executing step: {} for execution: {}", stepConfig.getStepName(), execution.getId());

            StepResult result;
            try {
                CircuitBreaker breaker = circuitBreakerRegistry.getBreaker(stepConfig.getStepName());
                result = breaker.execute(() -> executor.execute(execution), stepConfig.getStepName());            } catch (Exception e) {
                log.error("Unhandled exception in step: {} for execution: {}", stepConfig.getStepName(), execution.getId(), e);
                int retryCount = execution.getRetryCount();
                
                if (retryCount >= stepConfig.getRetryLimit()) {
                    log.error("Step permanently FAILED due to exception: {} for execution: {}, sending to DLQ", 
                            stepConfig.getStepName(), execution.getId());
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setFailureReason("Step failed with exception: " + e.getMessage());
                    saveAndPublish(execution);
                    deadLetterHandler.handle(execution);
                    return;
                } else {
                    long delay = retryPolicy.delay(retryCount);
                    log.warn("Step FAILED with exception: {} for execution: {}, retry count: {}, retrying after {}ms",
                            stepConfig.getStepName(), execution.getId(), retryCount, delay);
                    execution.setRetryCount(retryCount + 1);
                    execution.setStatus(ExecutionStatus.WAITING_RETRY);
                    execution.setFailureReason("Step failed with exception: " + e.getMessage());
                    saveAndPublish(execution);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_QUEUE, execution.getId(),
                            message -> {
                                message.getMessageProperties().setExpiration(String.valueOf(delay));
                                return message;
                            });
                    return;
                }
            }

            if (result == StepResult.SUCCESS) {
                if (i + 1 < steps.size()) {
                    execution.setCurrentStep(steps.get(i + 1).getStepName());
                }
                // Reset retry count for the next step
                execution.setRetryCount(0);
                execution.setFailureReason(null);
                idempotencyService.markAsExecuted(execution.getId(), stepConfig.getStepName());
                saveAndPublish(execution);
                log.info("Step SUCCESS: {} for execution: {}", stepConfig.getStepName(), execution.getId());

            } else {
                int retryCount = execution.getRetryCount();
                if (retryCount >= stepConfig.getRetryLimit()){
                    log.error("Step permanently FAILED: {} for execution: {}, sending to DLQ", stepConfig.getStepName(), execution.getId());
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setFailureReason("Step failed after " + retryCount + " retries");
                    saveAndPublish(execution);
                    deadLetterHandler.handle(execution);
                    return;
                }
                else {
                    long delay = retryPolicy.delay(retryCount);
                    log.warn("Step FAILED: {} for execution: {}, retry count: {}, retrying after {}ms",
                            stepConfig.getStepName(), execution.getId(), retryCount, delay);
                    execution.setRetryCount(retryCount+1);
                    execution.setStatus(ExecutionStatus.WAITING_RETRY);
                    execution.setFailureReason("Step execution failed, retry " + (retryCount + 1) + " of " + stepConfig.getRetryLimit());
                    saveAndPublish(execution);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_QUEUE, execution.getId(),
                            message -> {message.getMessageProperties()
                                    .setExpiration(String.valueOf(delay));
                        return message;
                    });
                    return;
                }
            }
        }
        log.info("Workflow COMPLETED for execution: {}", execution.getId());

        execution.setStatus(ExecutionStatus.COMPLETED);
        saveAndPublish(execution);
    }

    private void saveAndPublish(WorkflowExecution execution) {
        executionRepository.save(execution);
        publisher.publish(ExecutionStatusEvent.builder()
                .executionId(execution.getId())
                .workflowName(execution.getWorkflowName())
                .currentStep(execution.getCurrentStep())
                .status(execution.getStatus())
                .timestamp(Instant.now())
                .build());
    }
}
