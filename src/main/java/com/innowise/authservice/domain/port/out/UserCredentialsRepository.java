package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.UserCredentials;
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
    @Query("UPDATE UserCredentials uc SET uc.active = :active WHERE uc.userId = :userId")
    void setActiveByUserId(@Param("userId") Long userId, @Param("active") boolean active);
}
