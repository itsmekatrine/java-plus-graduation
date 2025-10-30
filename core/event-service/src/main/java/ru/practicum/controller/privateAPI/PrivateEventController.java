package ru.practicum.controller.privateAPI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.feign.RequestClient;
import ru.practicum.parameters.EventUserSearchParam;
import ru.practicum.service.EventService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users/{userId}/events")
@Validated
public class PrivateEventController {

    private final EventService eventService;
    private final RequestClient requestClient;

    @GetMapping
    public List<EventShortDto> getUsersEvents(@PathVariable @Positive Long userId,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                              @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Getting events by user id={}, from={}, size={}", userId, from, size);
        EventUserSearchParam params = EventUserSearchParam.builder()
                .userId(userId)
                .from(from)
                .size(size)
                .build();
        return eventService.getUsersEvents(params);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @RequestBody @Valid NewEventDto dto) {
        log.info("Saving new event {}", dto);
        return eventService.saveEvent(dto, userId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByUserIdAndEventId(@PathVariable @Positive Long userId,
                                                   @PathVariable @Positive Long eventId) {
        log.info("Getting event by userId={}, eventId={}", userId, eventId);
        return eventService.getEventByIdAndUserId(eventId, userId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByUser(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long eventId,
                                          @RequestBody @Valid UpdateEventUserRequest event) {
        log.info("Updating event id={} by user id={}", userId, eventId);
        return eventService.updateEventByUser(eventId, userId, event);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        log.info("Get requests for eventId={} by userId={}", eventId, userId);
        return requestClient.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateEventRequests(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody @Valid EventRequestStatusUpdateRequest body) {
        log.info("Update requests for eventId={} by userId={}, body={}", eventId, userId, body);
        return requestClient.updateEventRequests(userId, eventId, body);
    }
}
