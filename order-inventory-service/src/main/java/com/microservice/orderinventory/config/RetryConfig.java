package com.microservice.orderinventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry is enabled via @EnableRetry annotation
    // Retry behavior is configured on individual service methods using @Retryable
}
