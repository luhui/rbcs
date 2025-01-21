package com.example.rbcs;

import com.example.rbcs.infrastructure.sqs.EventQueuesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(EventQueuesProperties.class)
public class RbcsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RbcsApplication.class, args);
	}

}
