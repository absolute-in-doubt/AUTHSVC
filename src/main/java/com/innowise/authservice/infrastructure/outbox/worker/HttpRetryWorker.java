package com.innowise.authservice.infrastructure.outbox.worker;


import com.innowise.authservice.application.dto.CreateUserDto;
import com.innowise.authservice.domain.model.UserStatus;
import com.innowise.authservice.domain.port.out.UserCredentialsRepository;
import com.innowise.authservice.infrastructure.outbox.mapper.CreateUserRequestMapper;
import com.innowise.authservice.infrastructure.outbox.model.CreateUserOutboxEntity;
import com.innowise.authservice.infrastructure.outbox.model.OutboxEventStatus;
import com.innowise.authservice.infrastructure.outbox.out.CreateUserOutboxRepository;
import com.innowise.authservice.infrastructure.outbox.out.UserServiceClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/*
Http retry worker:
1. send requests to create user on user service
2. if (duplicate requests arrive) userId won't let to create duplicate users) <- idempotency key
3. as the success response received -> set UserCredentials status as ACTIVE
4. if after several retries (in one transaction) we haven't received the successful response,
increment the outbox event's retries counter
if (the counter is greater than some threshold) mark the outbox event as DEAD_LETTER and the UserCredentials as FAILED_TO_ACTIVATE
 */


@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRetryWorker {

    private final CreateUserOutboxRepository createUserOutboxRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final UserServiceClient userServiceClient;
    private final CreateUserRequestMapper mapper;

//NOTE, that there are also retries in the UserServiceClient for the request send
//Those are just worker's retries & only worker retries amount is stored as CreateUserOutboxEntity fields
    @Value("${application.outbox.workerRetriesThreshold}")
    private int workerRetriesThreshold;

    @Scheduled(fixedRateString = "${application.task.scheduling.outboxRetryWorker.rateMillis}")
    @Transactional
    public void process() {

        Optional<CreateUserOutboxEntity> entityOpt = createUserOutboxRepository.findUnprocessedAndUnlocked();

        if(entityOpt.isEmpty())
            return;

        CreateUserOutboxEntity createUserOutboxEntity = entityOpt.get();

        CreateUserDto requestDto = mapper.toDto(createUserOutboxEntity);

        ResponseEntity<Void> response;
        try {
            response = userServiceClient.createUser(requestDto);
        } catch (CallNotPermittedException e){
            log.trace("Failed to send a request to create user with userId: {} due to open circuit breaker", requestDto.userId());
            //NOTE: the retries counter isn't incremented.
            //We may not account the send attempt that have actually triggered the Circuit Breaker to open
            //(such precision isn't necessary here)
            return;
        }

        if(response.getStatusCode().equals(HttpStatus.CREATED)){
            userCredentialsRepository.setStatusByUserId(createUserOutboxEntity.getUserId(), UserStatus.ACTIVE);
            createUserOutboxRepository.setStatusByUserId(createUserOutboxEntity.getUserId(), OutboxEventStatus.COMPLETED);
        } else {
            if(createUserOutboxEntity.getRetriesCounter() >= workerRetriesThreshold)
                createUserOutboxRepository.setStatusByUserId(createUserOutboxEntity.getUserId(), OutboxEventStatus.DEAD_LETTER);
            else
                createUserOutboxRepository.incrementRetriesCounterByUserId(createUserOutboxEntity.getUserId());
        }
    }

}
