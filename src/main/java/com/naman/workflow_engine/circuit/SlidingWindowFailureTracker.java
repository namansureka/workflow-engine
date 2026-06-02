package com.naman.workflow_engine.circuit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlidingWindowFailureTracker {

    private static final long WINDOW_MS = 60*1000;;
    private final RedisTemplate<String, String> redisTemplate;

    public void recordSuccess(String serviceId) {

        String requestKey = requestKey(serviceId);
        long timestamp = System.currentTimeMillis();
        String member = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(requestKey,member, timestamp);

    }

    public void recordFailure(String serviceId) {
        String requestKey = requestKey(serviceId);
        String failureKey = failureKey(serviceId);

        long timestamp = System.currentTimeMillis();
        String member = UUID.randomUUID().toString();

        redisTemplate.opsForZSet().add(requestKey,member, timestamp);
        redisTemplate.opsForZSet().add(failureKey,member, timestamp);

    }

    public double getFailureRate(String serviceId) {
        cleanupOldEntries(serviceId);
        Long totalRequests = redisTemplate.opsForZSet().zCard(requestKey(serviceId));
        Long failures = redisTemplate.opsForZSet().zCard(failureKey(serviceId));
        if (totalRequests == 0) return 0.0;
        return (double) failures / totalRequests;
    }

    private String requestKey(String serviceId) {
        return "requests:" + serviceId;
    }

    private String failureKey(String serviceId) {
        return "failures:" + serviceId;
    }

    private void cleanupOldEntries(String serviceId){
        long currentTime = System.currentTimeMillis();
        long cutoff = currentTime - WINDOW_MS;
        redisTemplate.opsForZSet().removeRangeByScore(requestKey(serviceId), 0, cutoff);
        redisTemplate.opsForZSet().removeRangeByScore(failureKey(serviceId), 0, cutoff);
    }
}
