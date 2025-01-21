package com.example.rbcs.web.controller;

import com.example.rbcs.domain.event.TransactionCreatedEvent;
import com.example.rbcs.domain.event.TransactionExecuteRequest;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 测试场景下，绕过sqs发送消费的流程，提高测试效率
 */
@Component
public class SpringEventSqsMockAdapter {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @MockitoBean
    private SqsTemplate sqsTemplate;

    @EventListener
    @Async
    public void handle(TransactionCreatedEvent event) {
        var traceId = MDC.get("traceId");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
        }
        final var finalTraceId = traceId;
        MDC.put("traceId", finalTraceId);
        eventPublisher.publishEvent(new TransactionExecuteRequest(event.getAggregateRoot().getId()));
    }
}
