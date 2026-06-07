package com.naman.workflow_engine.observability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepExecutionLogRepository extends JpaRepository<StepExecutionLog, Long> {
    List<StepExecutionLog> findByStepName(String stepName);
    List<StepExecutionLog> findByExecutionId(Long executionId);
}
