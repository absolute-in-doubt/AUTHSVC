package com.innowise.authservice.infrastructure.outbox.mapper;

import com.innowise.authservice.domain.event.CreateUserEvent;
import com.innowise.authservice.infrastructure.outbox.model.CreateUserOutboxEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CreateUserEventMapper {

    @Mapping(target = "status", constant = "UNPROCESSED")
    CreateUserOutboxEntity toEntity(CreateUserEvent event);
}
