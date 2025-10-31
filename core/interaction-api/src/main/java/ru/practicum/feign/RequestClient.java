package ru.practicum.feign;

import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    // ADMIN
    @GetMapping("/admin/requests/confirmed")
    Map<Long, List<ParticipationRequestDto>> getConfirmedRequests(@RequestParam List<Long> eventIds) throws FeignException;

    @GetMapping("/admin/requests/count-by-event")
    Map<Long, Long> countByEvent(@RequestParam("eventIds") List<Long> eventIds,
                                 @RequestParam("status") String status) throws FeignException;

    @GetMapping("/admin/requests/exists")
    boolean exists(@RequestParam("userId") Long userId, @RequestParam("eventId") Long eventId, @RequestParam("status") String status) throws FeignException;

    // OWNER
    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                   @PathVariable Long eventId) throws FeignException;

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateEventRequests(@PathVariable Long userId,
                                                       @PathVariable Long eventId,
                                                       @RequestBody @Valid EventRequestStatusUpdateRequest body) throws FeignException;

    // REQUESTER
    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getMyRequests(@PathVariable Long userId) throws FeignException;

    @PostMapping("/users/{userId}/requests")
    ParticipationRequestDto create(@PathVariable("userId") Long userId,
                                   @RequestParam("eventId") Long eventId) throws FeignException;

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    ParticipationRequestDto cancel(@PathVariable Long userId,
                                   @PathVariable Long requestId) throws FeignException;
}
