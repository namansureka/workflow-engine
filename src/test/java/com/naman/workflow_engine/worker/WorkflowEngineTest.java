package com.naman.workflow_engine.worker;

import com.naman.workflow_engine.circuit.CircuitBreaker;
import com.naman.workflow_engine.circuit.CircuitBreakerRegistry;
import com.naman.workflow_engine.dashboard.WebSocketEventPublisher;
import com.naman.workflow_engine.job.model.ExecutionStatus;
import com.naman.workflow_engine.job.model.StepConfig;
import com.naman.workflow_engine.job.model.WorkflowDefinition;
import com.naman.workflow_engine.job.model.WorkflowExecution;
import com.naman.workflow_engine.job.repository.WorkflowExecutionRepository;
import com.naman.workflow_engine.job.service.WorkflowDefinitionService;
import com.naman.workflow_engine.observability.StepExecutionLogRepository;
import com.naman.workflow_engine.worker.idempotency.IdempotencyService;
import com.naman.workflow_engine.worker.retry.DeadLetterHandler;
import com.naman.workflow_engine.worker.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkflowEngineTest {

    @Mock
    private WorkflowDefinitionService definitionService;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @Mock
    private StepExecutorRegistry registry;

    @Mock
    private RetryPolicy retryPolicy;

    @Mock
    private DeadLetterHandler deadLetterHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private WebSocketEventPublisher publisher;

    @Mock
    private StepExecutionLogRepository stepExecutionLogRepository;

    @InjectMocks
    private WorkflowEngine workflowEngine;

    private WorkflowExecution execution;
    private WorkflowDefinition definition;
    private static final Long EXECUTION_ID = 1L;
    private static final String WORKFLOW_NAME = "testWorkflow";
    private static final String STEP_NAME = "sendEmail";

    @BeforeEach
    void setUp() {
        execution = new WorkflowExecution();
        execution.setId(EXECUTION_ID);
        execution.setWorkflowName(WORKFLOW_NAME);
        execution.setCurrentStep(STEP_NAME);
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setRetryCount(0);

        definition = new WorkflowDefinition();
        definition.setName(WORKFLOW_NAME);

        StepConfig stepConfig = new StepConfig();
        stepConfig.setStepName(STEP_NAME);
        stepConfig.setRetryLimit(3);

        definition.setSteps(List.of(stepConfig));
    }

    @Test
    void shouldCompleteWorkflowWhenAllStepsSucceed() {
        // Setup mocks
        when(definitionService.getDefinition(WORKFLOW_NAME)).thenReturn(definition);
        when(idempotencyService.isAlreadyExecuted(EXECUTION_ID, STEP_NAME)).thenReturn(false);

        StepExecutor executor = mock(StepExecutor.class);
        when(registry.getExecutor(STEP_NAME)).thenReturn(executor);
        when(executor.execute(execution)).thenReturn(StepResult.SUCCESS);

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreakerRegistry.getBreaker(STEP_NAME)).thenReturn(circuitBreaker);
        when(circuitBreaker.execute(any(Supplier.class), anyString())).thenAnswer(invocation -> {
            Supplier<StepResult> action = invocation.getArgument(0);
            return action.get();
        });

        // Execute
        workflowEngine.execute(execution);

        // Assert
        assertEquals(ExecutionStatus.COMPLETED, execution.getStatus());
        verify(executionRepository, times(3)).save(execution); // RUNNING, after step success, COMPLETED
        verify(deadLetterHandler, never()).handle(any());
    }

    @Test
    void shouldSetWaitingRetryOnStepFailure() {
        // Setup mocks
        when(definitionService.getDefinition(WORKFLOW_NAME)).thenReturn(definition);
        when(idempotencyService.isAlreadyExecuted(EXECUTION_ID, STEP_NAME)).thenReturn(false);

        StepExecutor executor = mock(StepExecutor.class);
        when(registry.getExecutor(STEP_NAME)).thenReturn(executor);
        when(executor.execute(execution)).thenReturn(StepResult.FAILURE);

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreakerRegistry.getBreaker(STEP_NAME)).thenReturn(circuitBreaker);
        when(circuitBreaker.execute(any(Supplier.class), anyString())).thenAnswer(invocation -> {
            Supplier<StepResult> action = invocation.getArgument(0);
            return action.get();
        });

        when(retryPolicy.delay(0)).thenReturn(2000L);
        execution.setRetryCount(0);

        // Execute
        workflowEngine.execute(execution);

        // Assert
        assertEquals(ExecutionStatus.WAITING_RETRY, execution.getStatus());
        assertEquals(1, execution.getRetryCount());
        verify(rabbitTemplate).convertAndSend(anyString(), any(Long.class), isA(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void shouldSendToDeadLetterAfterMaxRetries() {
        // Setup mocks
        when(definitionService.getDefinition(WORKFLOW_NAME)).thenReturn(definition);
        when(idempotencyService.isAlreadyExecuted(EXECUTION_ID, STEP_NAME)).thenReturn(false);

        StepExecutor executor = mock(StepExecutor.class);
        when(registry.getExecutor(STEP_NAME)).thenReturn(executor);
        when(executor.execute(execution)).thenReturn(StepResult.FAILURE);

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(circuitBreakerRegistry.getBreaker(STEP_NAME)).thenReturn(circuitBreaker);
        when(circuitBreaker.execute(any(Supplier.class), anyString())).thenAnswer(invocation -> {
            Supplier<StepResult> action = invocation.getArgument(0);
            return action.get();
        });

        execution.setRetryCount(3); // Already at limit

        // Execute
        workflowEngine.execute(execution);

        // Assert
        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertNotNull(execution.getFailureReason());
        verify(deadLetterHandler, times(1)).handle(execution);
    }

    @Test
    void shouldSkipStepIfAlreadyExecuted() {
        // Setup mocks
        when(definitionService.getDefinition(WORKFLOW_NAME)).thenReturn(definition);
        when(idempotencyService.isAlreadyExecuted(EXECUTION_ID, STEP_NAME)).thenReturn(true);

        StepExecutor executor = mock(StepExecutor.class);
        when(registry.getExecutor(STEP_NAME)).thenReturn(executor);

        // Execute
        workflowEngine.execute(execution);

        // Assert
        assertEquals(ExecutionStatus.COMPLETED, execution.getStatus());
        verify(executor, never()).execute(any());
        verify(deadLetterHandler, never()).handle(any());
    }

    @Test
    void shouldFailWhenStepExecutorNotFound() {
        // Setup mocks
        when(definitionService.getDefinition(WORKFLOW_NAME)).thenReturn(definition);
        when(registry.getExecutor(STEP_NAME)).thenReturn(null); // Executor not found

        // Execute
        workflowEngine.execute(execution);

        // Assert
        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertNotNull(execution.getFailureReason());
        verify(deadLetterHandler, times(1)).handle(execution);
    }
}










