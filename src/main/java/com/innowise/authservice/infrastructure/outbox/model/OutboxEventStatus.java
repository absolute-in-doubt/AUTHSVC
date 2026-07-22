package com.innowise.authservice.infrastructure.outbox.model;

public enum OutboxEventStatus {
    UNPROCESSED,
    COMPLETED,
    DEAD_LETTER
}
