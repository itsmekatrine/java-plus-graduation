package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Validated
public class ParticipationRequestInitiatorController {

    private final ParticipationRequestService service;

    @GetMapping
    public List<ParticipationRequestDto> list(@PathVariable @Positive Long userId,
                                              @PathVariable @Positive Long eventId) {
        log.info("List requests: userId={}, eventId={}", userId, eventId);
        return service.getRequestForEventByUserId(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateStatuses(
            @PathVariable("userId") @Positive Long userId,
            @PathVariable("eventId") @Positive Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest body) {
        log.info("Update request statuses: userId={}, eventId={}, body={}", userId, eventId, body);
        return service.updateRequest(userId, eventId, body);
    }
}
