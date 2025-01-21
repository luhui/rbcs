package com.example.rbcs.infrastructure.sqs;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqsPublisher {
    private final SqsTemplate sqsTemplate;
    private final EventQueuesProperties eventQueuesProperties;
    public void publish(Long transactionId) {
        var traceId = MDC.get("traceId");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
        }
        final var finalTraceId = traceId;
        sqsTemplate.send(to ->
                to.queue(eventQueuesProperties.getTransactionExecutionQueue())
                        .header("x-traceid", finalTraceId)
                        .payload(transactionId));
    }
}
