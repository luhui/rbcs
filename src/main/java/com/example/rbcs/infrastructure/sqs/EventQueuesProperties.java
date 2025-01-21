package com.example.rbcs.infrastructure.sqs;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "events.queues")
@Data
public class EventQueuesProperties {

    private String transactionExecutionQueue;
}
