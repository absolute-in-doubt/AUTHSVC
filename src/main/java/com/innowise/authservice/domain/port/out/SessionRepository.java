package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByIpAddressAndUserAgent(String ipAddress, String userAgent);

    @Query("UPDATE Session s SET s.active = :active WHERE s.sessionId = :sessionId")
    void setActive(@Param("sessionId") Long sessionId,@Param("active") boolean active);
}
