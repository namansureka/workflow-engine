package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowWorker {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowEngine workflowEngine;

    @RabbitListener(queues = RabbitMQConfig.WORKFLOW_QUEUE)
    public void processJob(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        workflowEngine.execute(execution);
    }
}
