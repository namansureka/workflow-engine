package com.naman.workflow_engine.job.service;

import com.naman.workflow_engine.common.dtos.WorkflowDefinitionRequest;
import com.naman.workflow_engine.common.exception.WorkflowNotFoundException;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;

    public WorkflowDefinition saveDefinition(WorkflowDefinitionRequest request) {
        WorkflowDefinition entity = definitionRepository.findByName(request.getName())
                .orElse(new WorkflowDefinition());
        entity.setName(request.getName());
        entity.setSteps(request.getSteps());
        return definitionRepository.save(entity);
    }
    public WorkflowDefinition getDefinition(String name) {
        return definitionRepository.findByName(name)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow definition not found: " + name));
    }
}