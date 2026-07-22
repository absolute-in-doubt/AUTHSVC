package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.UserCredentials;
import com.innowise.authservice.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {

    boolean existsByLogin(String login);

    Optional<UserCredentials> findByLogin(String login);

    @Modifying
    @Query("UPDATE UserCredentials SET status = :status WHERE userId = :userId")
    void setStatusByUserId(@Param("userId") Long userId, @Param("status") UserStatus status);
}
