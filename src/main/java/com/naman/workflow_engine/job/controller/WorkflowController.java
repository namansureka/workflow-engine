package com.naman.workflow_engine.job.controller;

import com.naman.workflow_engine.common.dtos.TriggerWorkflowRequest;
import com.naman.workflow_engine.common.dtos.WorkflowDefinitionRequest;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.service.WorkflowDefinitionService;
import com.naman.workflow_engine.job.service.WorkflowTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowTriggerService triggerService;
    private final WorkflowDefinitionService definitionService;

    @PostMapping("/define")
    public ResponseEntity<WorkflowDefinition> define(@RequestBody WorkflowDefinitionRequest request) {
        WorkflowDefinition saved = definitionService.saveDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);    }

    @PostMapping("/trigger")
    public ResponseEntity<WorkflowExecution> trigger(@RequestBody TriggerWorkflowRequest request) {
        WorkflowExecution execution=triggerService.triggerWorkflow(request.getWorkflowName());
        return ResponseEntity.ok(execution);    }
}