package com.naman.workflow_engine.common.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TriggerWorkflowRequest {

    private String workflowName;
}
