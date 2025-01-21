package com.example.rbcs.infrastructure.sqs;

import com.example.rbcs.domain.event.TransactionCreatedEvent;
import com.example.rbcs.domain.event.TransactionExecuteRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 事件适配器，把 spring 事件转换成 SQS 事件，SQS 事件再转换成spring  event
 * 这个类的作用是领域层和基础设施层之间解耦，项目过程中可以按需切换消息队列
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventSqsAdapter {
    private final SqsTemplate sqsTemplate;
    private final EventQueuesProperties eventQueuesProperties;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async
    public void handle(TransactionCreatedEvent event) {
        // 这里可以添加一些前置处理
        eventPublisher.publishEvent(new TransactionExecuteRequest(event.getAggregateRoot().getId()));
        sqsTemplate.send(to -> to.queue(eventQueuesProperties.getTransactionExecutionQueue()).payload(event.getAggregateRoot().getId()));
    }

    @SqsListener("${events.queues.transaction-execution-queue}")
    public void handle(String transactionId) {
        MDC.put("transactionId", transactionId);
        log.info("receive transaction execution request");
        eventPublisher.publishEvent(new TransactionExecuteRequest(Long.parseLong(transactionId)));
    }
}
