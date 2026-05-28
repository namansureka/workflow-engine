package com.naman.workflow_engine.worker.retry;


import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {

    public long delay(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000;
    }
}
