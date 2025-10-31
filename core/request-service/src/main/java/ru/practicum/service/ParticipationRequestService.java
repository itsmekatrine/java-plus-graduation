package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestMapper requestMapper;

    public List<ParticipationRequestDto> getRequestForEventByUserId(Long userId, Long eventId) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        userClient.getUserById(userId);
        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }
        userClient.getUserById(userId);

        EventFullDto event = eventClient.findEventById(eventId);

        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (initiatorId != null && initiatorId.equals(userId)) {
            throw new ConflictException("Initiator cannot request participation for own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event must be PUBLISHED");
        }

        Integer limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("Participant limit reached");
        }

        boolean moderationOff = Boolean.FALSE.equals(event.getRequestModeration());
        RequestStatus status;
        if (moderationOff || limit == 0) {
            if (limit > 0 && confirmed >= limit) {
                throw new ConflictException("Participant limit reached");
            }
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .requesterId(userId)
                .eventId(eventId)
                .status(status)
                .build();

        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!request.getEventId().equals(eventId)) {
                throw new NotFoundException("Request with requestId = " + request.getId() + "does not match eventId = " + eventId);
            }
        }

        int confirmedCount = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.CONFIRMED).size();
        int size = updateRequest.getRequestIds().size();
        int confirmedSize = updateRequest.getStatus().equals(RequestStatus.CONFIRMED) ? size : 0;

        if (event.getParticipantLimit() != 0 && confirmedCount + confirmedSize > event.getParticipantLimit()) {
            throw new ConflictException("Event limit exceed");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (updateRequest.getStatus().equals(RequestStatus.CONFIRMED)) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(requestMapper.toDto(request));
            } else if (updateRequest.getStatus().equals(RequestStatus.REJECTED)) {
                if (request.getStatus().equals(RequestStatus.CONFIRMED)) {
                    throw new ConflictException("The request cannot be rejected if it is confirmed");
                }
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userClient.getUserById(userId);
        ParticipationRequest r = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!Objects.equals(r.getRequesterId(), userId)) {
            throw new ConflictException("User is not the requester");
        }
        r.setStatus(RequestStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(r));
    }

    private void updateRequests(List<ParticipationRequest> requests, RequestStatus status, EventFullDto event) {
        boolean hasNotPending = requests.stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.PENDING);
        if (hasNotPending) throw new ConflictException("Can't change status when request status is not PENDING");

        final boolean moderation = Boolean.TRUE.equals(event.getRequestModeration());
        final Integer limitBoxed = event.getParticipantLimit();
        final boolean unlimited = (limitBoxed == null || limitBoxed == 0);

        if (status == RequestStatus.REJECTED) {
            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(requests);
            return;
        }

        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        for (ParticipationRequest r : requests) {
            if (!unlimited && confirmed >= limitBoxed) {
                throw new ConflictException("Requests out of limit");
            }
            r.setStatus(status);
            confirmed++;
        }
        requestRepository.saveAll(requests);
    }

    private static RequestStatus toModelStatus(ru.practicum.dto.request.RequestStatus s) {
        return RequestStatus.valueOf(s.name());
    }

    private void rejectAllPendingExcept(Long eventId, Set<Long> exceptIds, EventRequestStatusUpdateResult out) {
        List<ParticipationRequest> pendings = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
        for (ParticipationRequest p : pendings) {
            if (exceptIds.contains(p.getId())) continue;
            p.setStatus(RequestStatus.REJECTED);
        }
        requestRepository.saveAll(pendings);
        pendings.forEach(p -> out.getRejectedRequests().add(requestMapper.toDto(p)));
    }
}