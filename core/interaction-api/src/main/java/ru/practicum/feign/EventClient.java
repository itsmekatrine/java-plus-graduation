package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.comment.PreModerationRequest;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(name = "event-service", path = "/admin/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventFullDto findEventById(@PathVariable Long eventId);

    @GetMapping("/{userId}/{eventId}")
    EventFullDto getByUserIdAndEventId(@PathVariable Long userId, @PathVariable Long eventId);

    @PatchMapping("/{eventId}/pre-moderation")
    void appendForbiddenWords(@PathVariable Long eventId, @RequestBody PreModerationRequest body);
}
