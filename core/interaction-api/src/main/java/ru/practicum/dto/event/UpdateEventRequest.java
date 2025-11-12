package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventRequest {
    @Size(min = 20, max = 2000, message = "Annotation should be between 20 and 2000 characters long")
    String annotation;

    @Size(min = 20, max = 7000, message = "Description should be between 20 and 7000 characters long")
    String description;

    Long category;

    @Future
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    Location location;

    Boolean paid;

    @PositiveOrZero
    Integer participantLimit;

    Boolean requestModeration;

    @Size(min = 3, max = 120, message = "Title should be between 3 and 120 characters long")
    String title;
}
