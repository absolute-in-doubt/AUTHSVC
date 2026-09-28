package com.innowise.authservice.infrastructure.outbox.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "create_user_outbox")
@Data
@EntityListeners(AuditingEntityListener.class)
public class CreateUserOutboxEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;   //Idempotency key
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    private String email;
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    @Column(name = "retries_counter")
    private int retriesCounter;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
}
