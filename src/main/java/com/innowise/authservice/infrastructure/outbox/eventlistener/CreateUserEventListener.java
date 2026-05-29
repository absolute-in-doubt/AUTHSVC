package com.innowise.authservice.infrastructure.outbox.eventlistener;

import com.innowise.authservice.domain.event.CreateUserEvent;
import com.innowise.authservice.infrastructure.outbox.mapper.CreateUserEventMapper;
import com.innowise.authservice.infrastructure.outbox.out.CreateUserOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CreateUserEventListener {

    private final CreateUserOutboxRepository repository;

    private final CreateUserEventMapper mapper;


    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    public void  on(CreateUserEvent event){
        repository.save(mapper.toEntity(event));
    }
}
