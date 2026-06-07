package com.naman.workflow_engine.observability;

import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor

public class StuckWorkflowDetector {
    private final WorkflowExecutionRepository executionRepository;

    @Scheduled(fixedRate = 60000)
    public void detectStuckWorkflows() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<WorkflowExecution> runningExecutions = executionRepository
                .findByStatusIn(List.of(ExecutionStatus.RUNNING));

        for (WorkflowExecution execution : runningExecutions) {
            if (execution.getUpdatedAt().isBefore(threshold)) {
                log.warn("Stuck workflow detected: executionId={}, workflow={}, stuckSince={}",
                        execution.getId(),
                        execution.getWorkflowName(),
                        execution.getUpdatedAt());
            }
        }
    }
}
