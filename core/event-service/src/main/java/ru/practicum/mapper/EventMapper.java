package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;

@Mapper(componentModel = "spring", imports = {java.time.LocalDateTime.class})
public interface EventMapper {

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "location.lat", source = "lat")
    @Mapping(target = "location.lon", source = "lon")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "lat", expression = "java(dto.getLocation() != null ? dto.getLocation().getLat() : null)")
    @Mapping(target = "lon", expression = "java(dto.getLocation() != null ? dto.getLocation().getLon() : null)")
    @Mapping(target = "initiatorId", source = "userId")
    @Mapping(target = "category", source = "dto.category", qualifiedByName = "idToCategory")
    @Mapping(target = "state", expression = "java(ru.practicum.entity.EventState.PENDING)")

    @Mapping(target = "paid", expression = "java(Boolean.TRUE.equals(dto.getPaid()))")
    @Mapping(target = "participantLimit", expression = "java(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)")
    @Mapping(target = "requestModeration", expression = "java(dto.getRequestModeration() == null ? Boolean.TRUE : dto.getRequestModeration())")
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEntity(NewEventDto dto, Long userId);

    @Named("idToCategory")
    default Category idToCategory(Integer id) {
        if (id == null) return null;
        return Category.builder().id(id.longValue()).build();
    }

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        return Category.builder().id(id).build();
    }
}