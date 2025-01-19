package com.example.rbcs.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("transaction-service", r -> r.path("/api/transactions/**")
                        .filters(f -> f.addRequestHeader("X-User-Id", "${user.id}")
                                .addRequestHeader("X-User-Role", "${user.role}"))
                        .uri("lb://TRANSACTION-SERVICE"))
                .build();
    }
}