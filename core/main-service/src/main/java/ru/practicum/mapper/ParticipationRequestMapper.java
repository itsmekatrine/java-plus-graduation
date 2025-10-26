package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.entity.ParticipationRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParticipationRequestMapper {
    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requesterId")
    ParticipationRequestDto toDto(ParticipationRequest request);
}
