package com.naman.workflow_engine.circuit.fallback;

import com.naman.workflow_engine.circuit.FallbackStrategy;
import com.naman.workflow_engine.worker.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class CachedFallback implements FallbackStrategy {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public StepResult getFallback(String serviceId) {
        String key = "cache:" + serviceId + ":lastResponse";
        String cachedResponse = redisTemplate.opsForValue().get(key);
        if (cachedResponse != null) {
            log.warn("Cached fallback triggered for service: {}. " +
                          "Returning last known good response.", serviceId);
            log.debug(cachedResponse);
            return StepResult.SUCCESS;
        }
        log.warn("Cached fallback triggered for service: {} but no cached response found. " +
                         "Returning FAILURE.", serviceId);
        return StepResult.FAILURE;
    }
}
