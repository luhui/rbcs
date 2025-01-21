package com.example.rbcs.web.controller;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false")
public class MockSqs {
    @Bean
    SqsTemplate sqsTemplate() {
        return Mockito.mock(SqsTemplate.class);
    }
}
