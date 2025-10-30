package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestStatus;
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
        EventFullDto event = eventClient.findEventById(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event must be published to request participation");
        }
        if (event.getParticipantLimit() != null && event.getParticipantLimit() != 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Event participant limit reached");
            }
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        request.setCreated(LocalDateTime.now());

        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequest(Long eventId, Long userId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (!Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Can't update requests for event id=" + eventId + " by user id=" + userId);
        }
        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());
        updateRequests(requests, toModelStatus(updateRequest.getStatus()), event);

        List<ParticipationRequest> saved = requestRepository.saveAll(requests);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        for (ParticipationRequest r : saved) {
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
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("User is not the requester");
        }
        request.setStatus(RequestStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    private void updateRequests(List<ParticipationRequest> requests, RequestStatus targetStatus, EventFullDto event) {
        boolean hasNotPending = requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING);
        if (hasNotPending) {
            throw new ConflictException("Can't change status when request status is not PENDING");
        }

        if (targetStatus == RequestStatus.REJECTED) {
            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            return;
        }

        boolean moderation = Boolean.TRUE.equals(event.getRequestModeration());
        Integer limit = event.getParticipantLimit();
        boolean unlimited = (limit == null || limit == 0);

        if (!moderation && unlimited) {
            requests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
            return;
        }

        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        for (ParticipationRequest r : requests) {
            if (!unlimited && confirmed >= limit) {
                throw new ConflictException("Requests out of limit");
            }
            r.setStatus(RequestStatus.CONFIRMED);
            confirmed++;
        }
    }

    private static RequestStatus toModelStatus(ru.practicum.dto.request.RequestStatus s) {
        return RequestStatus.valueOf(s.name());
    }
}