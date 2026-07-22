package com.innowise.authservice.domain.model;

import com.innowise.authservice.domain.entityListener.ExpiresAtSessionEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@NoArgsConstructor
@EntityListeners(value = {AuditingEntityListener.class, ExpiresAtSessionEntityListener.class})
@Data
@Builder
@AllArgsConstructor
public class Session {
    @SequenceGenerator(name = "session_id_gen", sequenceName = "session_id_seq")

    @Id
    @GeneratedValue(generator = "session_id_gen")
    @Column(name="session_id")
    private Long sessionId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "user_agent")
    private String userAgent;
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
