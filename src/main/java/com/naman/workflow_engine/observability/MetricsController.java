package com.naman.workflow_engine.observability;

import com.naman.workflow_engine.common.dtos.StepMetricsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricsService metricsService;

     @GetMapping
     public ResponseEntity<StepMetricsDTO> getAverageExecutionTime(@RequestParam String stepName) {
            StepMetricsDTO metrics = metricsService.getStepMetrics(stepName);
            return ResponseEntity.ok(metrics);
        }

}
