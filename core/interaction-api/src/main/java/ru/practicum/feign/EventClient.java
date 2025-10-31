package ru.practicum.feign;

import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.comment.PreModerationRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/events/{eventId}")
    EventFullDto findEventById(@PathVariable("eventId") Long eventId) throws FeignException;

    @GetMapping("/users/{userId}/events/{eventId}")
    EventFullDto getByUserIdAndEventId(@PathVariable("userId") Long userId, @PathVariable("eventId") Long eventId) throws FeignException;

    @PatchMapping("/admin/events/{eventId}/pre-moderation")
    void appendForbiddenWords(@PathVariable("eventId") Long eventId, @RequestBody PreModerationRequest body) throws FeignException;

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateEventRequests(@PathVariable Long userId,
                                                       @PathVariable Long eventId,
                                                       @RequestBody @Valid EventRequestStatusUpdateRequest body) throws FeignException;

}
