package com.innowise.authservice.infrastructure.outbox.out;

import com.innowise.authservice.infrastructure.outbox.model.CreateUserOutboxEntity;
import com.innowise.authservice.infrastructure.outbox.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreateUserOutboxRepository extends JpaRepository<CreateUserOutboxEntity, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM create_user_outbox WHERE status = 'UNPROCESSED' FOR UPDATE SKIP LOCKED
            """)
    Optional<CreateUserOutboxEntity> findUnprocessedAndUnlocked();

    @Modifying
    @Query("UPDATE CreateUserOutboxEntity cuoe SET cuoe.status = :status WHERE cuoe.userId = :userId")
    void setStatusByUserId(@Param("userId") Long userId, @Param("status") OutboxEventStatus status);

    @Modifying
    @Query("UPDATE CreateUserOutboxEntity cuoe SET cuoe.retriesCounter = retriesCounter + 1 WHERE cuoe.userId = :userId")
    void incrementRetriesCounterByUserId(@Param("userId") Long userId);
}
