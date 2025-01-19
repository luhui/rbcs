package com.example.rbcs.infrastructure.threads;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Getter
@ToString
@EqualsAndHashCode
@Configuration
public class ThreadPoolConfig implements AsyncConfigurer {
    @Value("${rbcs.thread.pool.core.size:10}")
    private Integer corePoolSize;
    @Value("${rbcs.thread.pool.max.size:20}")
    private Integer maxPoolSize;
    @Value("${rbcs.thread.pool.queue.capacity:100}")
    private Integer pollQueueCapacity;
    @Value("${rbcs.thread.pool.keep.alive:60}")
    private Integer keepAliveSeconds;

    /**
     * 为系统配置默认的AsyncExecutor，统一线程池相关的配置
     * 业务系统作为IO密集型应用，应该尽可能少的使用自定义线程，避免产生一致性问题，所以对外统一一个executor，可以减少业务层不合理的配置导致的性能问题
     */
    @Override
    public Executor getAsyncExecutor() {
        val executor = new ContextThreadPoolExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(pollQueueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 设置线程池的拒绝策略：默认的AbortPolicy, 拿不到就立即丢弃，不符合我们实际场景。
        // 修改成CallerRunsPolicy，该策略较为平缓策略，当线程数达到上限且等待队列达到上限时，交由主线程去执行。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
