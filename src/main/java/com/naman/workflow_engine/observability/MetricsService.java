package com.naman.workflow_engine.observability;

import com.naman.workflow_engine.common.dtos.StepMetricsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final StepExecutionLogRepository stepExecutionLogRepository;
    public StepMetricsDTO getStepMetrics(String stepName){
        var logs = stepExecutionLogRepository.findByStepName(stepName);
        long totalExecutions = logs.size();
        long successCount = logs.stream().filter(log -> "SUCCESS".equals(log.getOutcome())).count();
        long failureCount = logs.stream().filter(log -> "FAILURE".equals(log.getOutcome())).count();
        double avgDuration = logs.stream().mapToLong(StepExecutionLog::getDurationMs).average().orElse(0);
        double successRate = totalExecutions == 0 ? 0 : (double) successCount / totalExecutions * 100;
        return new StepMetricsDTO(stepName, totalExecutions, successCount, failureCount, successRate, avgDuration);
    }


}
