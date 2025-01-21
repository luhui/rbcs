package com.example.rbcs.infrastructure.sqs;

import com.example.rbcs.domain.event.TransactionCreatedEvent;
import com.example.rbcs.domain.event.TransactionExecuteRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 事件适配器，把 spring 事件转换成 SQS 事件，SQS 事件再转换成spring  event
 * 这个类的作用是领域层和基础设施层之间解耦，项目过程中可以按需切换消息队列
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventSqsAdapter {
    private final SqsPublisher sqsPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
    @EventListener
    public void handle(TransactionCreatedEvent event) {
        sqsPublisher.publish(event.getAggregateRoot().getId());
    }

    @SqsListener("${events.queues.transaction-execution-queue}")
    public void handle(Message message) {
        var transactionId = message.body();
        final var traceId = message.messageAttributes().getOrDefault("x-traceid", MessageAttributeValue.builder().stringValue(UUID.randomUUID().toString().replaceAll("-", "")).build()).stringValue();
        MDC.put("traceId", traceId);
        MDC.put("transactionId", transactionId);
        log.info("receive transaction execution request");
        eventPublisher.publishEvent(new TransactionExecuteRequest(Long.parseLong(transactionId)));
    }
}
