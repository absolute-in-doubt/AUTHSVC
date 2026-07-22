package com.innowise.authservice.domain.entityListener;

import com.innowise.authservice.domain.model.Session;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ExpiresAtSessionEntityListener {

    @Value("${application.security.sessionLengthMinutes}")
    private long sessionLengthMinutes;

    @PrePersist
    public void prePersist(Session session) {
        if (session.getExpiresAt() == null) {
            session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionLengthMinutes));
        }
    }
}
