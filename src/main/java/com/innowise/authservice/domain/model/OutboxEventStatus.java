package com.innowise.authservice.domain.model;

public enum OutboxEventStatus {
    UNPROCESSED,
    COMPLETED,
    DEAD_LETTER
}
