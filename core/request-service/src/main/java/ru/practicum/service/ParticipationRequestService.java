package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestStatus;
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
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (!Objects.equals(initiatorId, userId)) {
            throw new ConflictException("User " + userId + " is not the initiator of event " + eventId);
        }
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
        userClient.getUserById(userId);
        EventFullDto event;
        try {
            event = eventClient.findEventById(eventId);
            if (event == null) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
        } catch (feign.FeignException.NotFound ex) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Event must be published to request participation");
        }

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit != 0 && confirmed >= limit) {
            throw new ConflictException("Event participant limit reached");
        }

        ParticipationRequest result = new ParticipationRequest();
        result.setRequesterId(userId);
        result.setEventId(eventId);

        boolean requiresModeration = Boolean.TRUE.equals(event.getRequestModeration());
        result.setStatus((limit == 0 || !requiresModeration) ? RequestStatus.CONFIRMED : RequestStatus.PENDING);
        result.setCreated(LocalDateTime.now());

        return requestMapper.toDto(requestRepository.save(result));
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (!Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Can't update requests for event id=" + eventId + " by user id=" + userId);
        }
        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new ConflictException("No requests to update");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        if (requests.stream().anyMatch(r -> !Objects.equals(r.getEventId(), eventId))) {
            throw new NotFoundException("Some requests do not belong to event id=" + eventId);
        }

        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Request must have status PENDING");
        }

        RequestStatus target = toModelStatus(updateRequest.getStatus());
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        boolean unlimited = (limit == 0);

        if (target == RequestStatus.REJECTED) {
            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
        } else if (target == RequestStatus.CONFIRMED) {
            for (ParticipationRequest r : requests) {
                if (!unlimited && confirmed >= limit) {
                    rejectAllPendingExcept(eventId, Set.copyOf(updateRequest.getRequestIds()), result);
                    throw new ConflictException("The participant limit has been reached");
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
            }
            if (!unlimited && confirmed >= limit) {
                rejectAllPendingExcept(eventId, Set.copyOf(updateRequest.getRequestIds()), result);
            }
        } else {
            throw new BadRequestException("Unsupported target status: " + target);
        }

        requestRepository.saveAll(requests);

        for (ParticipationRequest r : requests) {
            if (r.getStatus() == RequestStatus.CONFIRMED) {
                result.getConfirmedRequests().add(requestMapper.toDto(r));
            } else if (r.getStatus() == RequestStatus.REJECTED) {
                result.getRejectedRequests().add(requestMapper.toDto(r));
            }
        }
        return result;
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
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