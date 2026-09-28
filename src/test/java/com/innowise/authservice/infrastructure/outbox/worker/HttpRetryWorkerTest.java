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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpRetryWorkerTest {

    @Mock
    private CreateUserOutboxRepository createUserOutboxRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CreateUserRequestMapper mapper;

    @InjectMocks
    private HttpRetryWorker httpRetryWorker;

    private CreateUserOutboxEntity outboxEntity;
    private CreateUserDto createUserDto;
    private static final Long USER_ID = 1L;
    private static final int WORKER_RETRIES_THRESHOLD = 3;

    @BeforeEach
    void setUp() throws Exception {
        Field thresholdField = HttpRetryWorker.class.getDeclaredField("workerRetriesThreshold");
        thresholdField.setAccessible(true);
        thresholdField.set(httpRetryWorker, WORKER_RETRIES_THRESHOLD);

        outboxEntity = new CreateUserOutboxEntity();
        outboxEntity.setUserId(USER_ID);
        outboxEntity.setFirstName("John");
        outboxEntity.setLastName("Doe");
        outboxEntity.setBirthDate(LocalDate.of(1990, 1, 1));
        outboxEntity.setEmail("john.doe@example.com");
        outboxEntity.setStatus(OutboxEventStatus.UNPROCESSED);
        outboxEntity.setRetriesCounter(0);

        createUserDto = new CreateUserDto(
                USER_ID,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john.doe@example.com"
        );
    }

    @Test
    void process_whenNoUnprocessedEntities_shouldDoNothing() {
        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.empty());

        httpRetryWorker.process();

        verifyNoInteractions(userServiceClient, userCredentialsRepository, mapper);
        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), any());
        verify(createUserOutboxRepository, never()).incrementRetriesCounterByUserId(anyLong());
    }


    @Test
    void process_whenUserCreatedSuccessfully_shouldSetActiveAndCompletedStatus() {
        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.of(outboxEntity));
        when(mapper.toDto(outboxEntity)).thenReturn(createUserDto);
        when(userServiceClient.createUser(createUserDto)).thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        httpRetryWorker.process();

        verify(userCredentialsRepository).setStatusByUserId(USER_ID, UserStatus.ACTIVE);
        verify(createUserOutboxRepository).setStatusByUserId(USER_ID, OutboxEventStatus.COMPLETED);
        verify(createUserOutboxRepository, never()).incrementRetriesCounterByUserId(anyLong());
    }


    @Test
    void process_whenRequestFailsAndRetriesBelowThreshold_shouldIncrementRetriesCounter() {
        outboxEntity.setRetriesCounter(WORKER_RETRIES_THRESHOLD - 1); // One below threshold
        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.of(outboxEntity));
        when(mapper.toDto(outboxEntity)).thenReturn(createUserDto);
        when(userServiceClient.createUser(createUserDto)).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        httpRetryWorker.process();

        verify(createUserOutboxRepository).incrementRetriesCounterByUserId(USER_ID);
        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), eq(OutboxEventStatus.DEAD_LETTER));
        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), eq(OutboxEventStatus.COMPLETED));
        verify(userCredentialsRepository, never()).setStatusByUserId(anyLong(), any());
    }


    @Test
    void process_whenRequestFailsAndRetriesExceedsThreshold_shouldMarkAsDeadLetter() {
        outboxEntity.setRetriesCounter(WORKER_RETRIES_THRESHOLD); // At threshold
        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.of(outboxEntity));
        when(mapper.toDto(outboxEntity)).thenReturn(createUserDto);
        when(userServiceClient.createUser(createUserDto)).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        httpRetryWorker.process();

        verify(createUserOutboxRepository).setStatusByUserId(USER_ID, OutboxEventStatus.DEAD_LETTER);
        verify(createUserOutboxRepository, never()).incrementRetriesCounterByUserId(anyLong());
        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), eq(OutboxEventStatus.COMPLETED));
        verify(userCredentialsRepository, never()).setStatusByUserId(anyLong(), any());
    }


    @Test
    void process_whenCircuitBreakerOpen_shouldLogAndReturnWithoutChanges() {
        String CB_NAME = "userService";
        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.of(outboxEntity));
        when(mapper.toDto(outboxEntity)).thenReturn(createUserDto);
        when(userServiceClient.createUser(createUserDto))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults(CB_NAME)));

        httpRetryWorker.process();

        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), any());
        verify(createUserOutboxRepository, never()).incrementRetriesCounterByUserId(anyLong());
        verify(userCredentialsRepository, never()).setStatusByUserId(anyLong(), any());
    }


    @Test
    void process_whenUnexpectedException_shouldPropagateException() {

        when(createUserOutboxRepository.findUnprocessedAndUnlocked()).thenReturn(Optional.of(outboxEntity));
        when(mapper.toDto(outboxEntity)).thenReturn(createUserDto);
        when(userServiceClient.createUser(createUserDto)).thenThrow(new RuntimeException("Unexpected error"));


        assertThrows(RuntimeException.class, () -> httpRetryWorker.process());

        verify(createUserOutboxRepository, never()).setStatusByUserId(anyLong(), any());
        verify(createUserOutboxRepository, never()).incrementRetriesCounterByUserId(anyLong());
        verify(userCredentialsRepository, never()).setStatusByUserId(anyLong(), any());
    }
}
