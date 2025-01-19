package com.example.rbcs.infrastructure.threads;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ContextThreadPoolExecutor extends ThreadPoolTaskExecutor {
    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(wrapRunnable(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(wrapCallable(task));
    }

    @Override
    public void execute(Runnable task) {
        super.execute(wrapRunnable(task));
    }

    private <T> Callable<T> wrapCallable(Callable<T> task) {
        final String traceId = MDC.get("traceId");
        final String userId = MDC.get("userId");
        final String transactionId = MDC.get("transactionId");
        return TtlCallable.get(() -> {
            MDC.put("traceId", traceId);
            MDC.put("userId", userId);
            MDC.put("transactionId", transactionId);
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        });
    }

    private Runnable wrapRunnable(Runnable task) {
        final String traceId = MDC.get("traceId");
        final String userId = MDC.get("userId");
        final String transactionId = MDC.get("transactionId");
        return TtlRunnable.get(() -> {
            MDC.put("traceId", traceId);
            MDC.put("userId", userId);
            MDC.put("transactionId", transactionId);
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        });
    }
}

