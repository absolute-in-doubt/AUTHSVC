package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByIpAddressAndUserAgent(String ipAddress, String userAgent);

    @Modifying
    @Query("UPDATE Session SET active = :active WHERE sessionId = :sessionId")
    void setActive(@Param("sessionId") Long sessionId, @Param("active") boolean active);

    @Query("SELECT s FROM Session s WHERE s.refreshTokenHash = :rth and s.active = true")
    Optional<Session> findByRefreshTokenHashAndActiveTrue(@Param("rth") String refreshTokenHash);

    @Modifying
    @Query("UPDATE Session SET refreshTokenHash = :rth WHERE sessionId = :sessionId")
    void updateRefreshTokenHash(@Param("sessionId") Long sessionId, @Param("rth") String refreshTokenHash);

    @Modifying
    @Query("UPDATE Session SET active = false WHERE userId = :userId")
    void setActiveFalseByUserId(@Param("userId") Long userId);
}
