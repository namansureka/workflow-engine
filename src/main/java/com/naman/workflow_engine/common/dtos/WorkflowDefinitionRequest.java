package com.naman.workflow_engine.common.dtos;

import com.naman.workflow_engine.job.model.StepConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionRequest {
    private String name;
    private List<StepConfig> steps;

}
