package ru.practicum.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {
    String annotation;
    CategoryDto category;
    Long confirmedRequests;
    LocalDateTime eventDate;
    Long id;
    UserShortDto initiator;
    Boolean paid;
    String title;
    Double rating;
    LocalDateTime createdOn;
    String description;
    Location location;
    Integer participantLimit = 0;
    LocalDateTime publishedOn;
    Boolean requestModeration;
    EventState state;
    Set<String> forbiddenWords;
}
