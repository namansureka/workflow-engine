package com.naman.workflow_engine.job.service;

import com.naman.workflow_engine.config.RabbitMQConfig;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowTriggerService {

    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowExecutionRepository executionRepository;
    private final RabbitTemplate rabbitTemplate;

    public WorkflowExecution triggerWorkflow(String workflowName) {

        WorkflowDefinition definition = workflowDefinitionService.getDefinition(workflowName);
        String firstStep = definition.getSteps().get(0).getStepName();

        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowName(workflowName);
        execution.setCurrentStep(firstStep);

        WorkflowExecution saved = executionRepository.save(execution);

        rabbitTemplate.convertAndSend(RabbitMQConfig.WORKFLOW_QUEUE, saved.getId());
        return saved;
    }
}
