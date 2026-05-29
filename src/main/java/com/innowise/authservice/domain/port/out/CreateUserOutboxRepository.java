package com.innowise.authservice.domain.port.out;

import com.innowise.authservice.domain.model.CreateUserOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreateUserOutboxRepository extends JpaRepository<CreateUserOutboxEvent, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM create_user_outbox WHERE status = 'UNPROCESSED' FOR UPDATE SKIP LOCKED
            """)
    Optional<CreateUserOutboxEvent> findUnprocessedAndUnlocked();

}
