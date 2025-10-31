package ru.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.comment.PreModerationRequest;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/events/{eventId}")
    EventFullDto findEventById(@PathVariable("eventId") Long eventId) throws FeignException;;

    @GetMapping("/users/{userId}/events/{eventId}")
    EventFullDto getByUserIdAndEventId(@PathVariable("userId") Long userId, @PathVariable("eventId") Long eventId) throws FeignException;;

    @PatchMapping("/admin/events/{eventId}/pre-moderation")
    void appendForbiddenWords(@PathVariable("eventId") Long eventId, @RequestBody PreModerationRequest body) throws FeignException;;
}
