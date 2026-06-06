package com.naman.workflow_engine.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(ExecutionStatusEvent event) {
        messagingTemplate.convertAndSend("/topic/executions", event);    }
}
