package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.BulkStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/requests")
@RequiredArgsConstructor
public class AdminRequestController {

    private final ParticipationRequestRepository repo;
    private final ParticipationRequestMapper mapper;

    @GetMapping("/count-by-event")
    public Map<Long, Long> countByEvent(@RequestParam("eventIds") List<Long> eventIds,
                                        @RequestParam("status") String status) {
        if (eventIds == null || eventIds.isEmpty()) return Map.of();
        RequestStatus st = RequestStatus.valueOf(status);
        return repo.countByEventIdsAndStatus(eventIds, st).stream()
                .collect(Collectors.toMap(
                        ParticipationRequestRepository.EventCountRow::getEventId,
                        ParticipationRequestRepository.EventCountRow::getCnt
                ));
    }

    @GetMapping("/confirmed")
    public Map<Long, List<ParticipationRequestDto>> getConfirmed(@RequestParam List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Map.of();
        List<ParticipationRequest> list = repo.findByEventIdInAndStatus(eventIds, RequestStatus.CONFIRMED);
        return list.stream().collect(Collectors.groupingBy(
                r -> r.getEventId(),
                Collectors.mapping(mapper::toDto, Collectors.toList())
        ));
    }

    @GetMapping("/exists")
    public boolean exists(@RequestParam("userId") Long userId,
                          @RequestParam("eventId") Long eventId,
                          @RequestParam("status") String status) {
        return repo.existsByRequesterIdAndEventIdAndStatus(userId, eventId, RequestStatus.valueOf(status));
    }

    @PatchMapping("/bulk-status")
    public List<ParticipationRequestDto> bulkStatus(@RequestBody BulkStatusUpdateRequest body) {
        List<ParticipationRequest> list = repo.findAllById(body.getRequestIds());
        boolean ok = list.stream().allMatch(r -> r.getEventId().equals(body.getEventId()));
        if (!ok) throw new ConflictException("All requestIds must belong to the event");

        RequestStatus newStatus = RequestStatus.valueOf(body.getStatus()); // "CONFIRMED"/"REJECTED"
        list.forEach(r -> r.setStatus(newStatus));
        repo.saveAll(list);
        return list.stream().map(mapper::toDto).toList();
    }
}
