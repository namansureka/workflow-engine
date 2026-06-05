package com.naman.workflow_engine.common.dtos;

import com.naman.workflow_engine.circuit.CircuitBreakerState;
import lombok.Data;

import java.time.Instant;

@Data
public class CircuitBreakerResponse {
    String serviceId;
    CircuitBreakerState state;
    Instant openedTime;
    int failurePressure;
}
