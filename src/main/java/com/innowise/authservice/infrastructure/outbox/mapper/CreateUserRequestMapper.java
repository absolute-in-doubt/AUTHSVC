package com.innowise.authservice.infrastructure.outbox.mapper;

import com.innowise.authservice.application.dto.CreateUserDto;
import com.innowise.authservice.infrastructure.outbox.model.CreateUserOutboxEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CreateUserRequestMapper {

    CreateUserDto toDto(CreateUserOutboxEntity entity);
}
