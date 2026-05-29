package com.naman.workflow_engine.worker.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;

    private String buildKey(Long executionId, String stepName) {
        return "step:" + executionId + ":" + stepName;
    }

    public boolean isAlreadyExecuted(Long executionId, String stepName) {
        return redisTemplate.hasKey(buildKey(executionId, stepName));
    }

    public void markAsExecuted(Long executionId, String stepName) {
        redisTemplate.opsForValue().set(buildKey(executionId, stepName), "executed");
    }
}
