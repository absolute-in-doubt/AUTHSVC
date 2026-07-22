package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s WHERE s.userId = :userId and s.ipAddress = :ipAddress and s.userAgent = :userAgent and s.active = true")
    Optional<Session> findByUserIdAndIpAddressAndUserAgentAndActiveTrue(@Param("userId") Long userId, @Param("ipAddress") String ipAddress, @Param("userAgent") String userAgent);

    @Modifying
    @Query("UPDATE Session SET active = :active WHERE sessionId = :sessionId")
    void setActive(@Param("sessionId") Long sessionId, @Param("active") boolean active);



    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT s FROM Session s WHERE s.userId = :userId and s.active = true")
    List<Session> findByUserIdAndActiveTrue(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Session SET refreshTokenHash = :rth WHERE sessionId = :sessionId")
    void updateRefreshTokenHash(@Param("sessionId") Long sessionId, @Param("rth") String refreshTokenHash);

    @Modifying
    @Query("UPDATE Session SET active = false WHERE userId = :userId")
    void setActiveFalseByUserId(@Param("userId") Long userId);
}
