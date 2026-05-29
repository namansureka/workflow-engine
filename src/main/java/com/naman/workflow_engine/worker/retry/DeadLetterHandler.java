package com.naman.workflow_engine.worker.retry;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadLetterHandler {

    private final WorkflowExecutionRepository executionRepository;
    private final RabbitTemplate rabbitTemplate;

    public void handle(WorkflowExecution execution){

        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFailureReason("Max retries exhausted");
        executionRepository.save(execution);
        rabbitTemplate.convertAndSend(RabbitMQConfig.DLQ,execution.getId());
    }
}
