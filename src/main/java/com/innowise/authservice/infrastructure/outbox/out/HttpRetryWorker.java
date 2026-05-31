package com.innowise.authservice.infrastructure.outbox.out;


import com.innowise.authservice.application.dto.CreateUserDto;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.port.out.UserCredentialsRepository;
import com.innowise.authservice.infrastructure.outbox.mapper.CreateUserRequestMapper;
import com.innowise.authservice.infrastructure.outbox.model.CreateUserOutboxEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class HttpRetryWorker {

    private final CreateUserOutboxRepository createUserOutboxRepository;
    private final UserCredentialsRepository userCredentialsRepository;

    private final CreateUserRequestMapper mapper;





    @Scheduled(fixedRateString = "${application.task.scheduling.outboxRetryWorker.rateMillis}")
    @Transactional
    public void process() {

        Optional<CreateUserOutboxEntity> entityOpt = createUserOutboxRepository.findUnprocessedAndUnlocked();

        if(entityOpt.isEmpty())
            return;

        CreateUserOutboxEntity createUserOutboxEntity = entityOpt.get();

        CreateUserDto requestDto = mapper.toDto(createUserOutboxEntity);



        if(response.getStatusCode().equals(HttpStatus.CREATED)){

        }
        /*
    TODO create a retry worker
    Http retry worker:
    1. send requests to create user on user service
    2. if (duplicate requests arrive) userId won't let to create duplicate users) <- idempotency key
    3. as the success response received -> set UserCredentials status as ACTIVE
    4. if after several retries (in one transaction) we haven't received the successful response,
    increment the outbox event's retries counter
    if (the counter is greater than some threshold) mark the outbox event as DEAD_LETTER and the UserCredentials as FAILED_TO_ACTIVATE
     */
    }

}
