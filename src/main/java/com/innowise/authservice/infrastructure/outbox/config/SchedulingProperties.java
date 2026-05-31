package com.innowise.authservice.infrastructure.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "spring.task.scheduling")
public record SchedulingProperties(
        int poolSize,
        String threadNamePrefix,
        Shutdown shutdown
) {

    public record Shutdown(
            long awaitTerminationPeriodSeconds
    ){}
}
