package com.naman.workflow_engine.job.service;

import com.naman.workflow_engine.common.dtos.WorkflowDefinitionRequest;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;

    public WorkflowDefinition saveDefinition(WorkflowDefinitionRequest request) {
        // 1. create a new WorkflowDefinition entity
        // 2. set name and steps from request
        // 3. save to DB and return
        WorkflowDefinition entity = new WorkflowDefinition();
        entity.setName(request.getName());
        entity.setSteps(request.getSteps());
        return definitionRepository.save(entity);
    }

    public WorkflowDefinition getDefinition(String name) {
        // 1. call repository findByName
        // 2. if not found → throw exception
        // 3. return the definition
        return definitionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Workflow definition not found: " + name));
    }
}