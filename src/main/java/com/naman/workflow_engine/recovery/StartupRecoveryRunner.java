package com.naman.workflow_engine.recovery;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupRecoveryRunner {

    private final WorkflowExecutionRepository executionRepository;
    private final RabbitTemplate rabbitTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {

        List<WorkflowExecution> executions=executionRepository.findByStatusIn
                        (List.of(ExecutionStatus.RUNNING,
                        ExecutionStatus.WAITING_RETRY));
        for(WorkflowExecution execution : executions){

            rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_QUEUE,execution.getId());
        }

    }
}
