package com.naman.workflow_engine.job.repository;

import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    List<WorkflowExecution> findByStatusIn(List<ExecutionStatus> statuses);
}
