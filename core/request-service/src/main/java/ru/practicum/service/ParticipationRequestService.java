package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;
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

    public List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) {
        eventClient.getByUserIdAndEventId(userId, eventId);
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
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
        RequestStatus status = RequestStatus.PENDING;

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }

        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event must be published to request participation");
        }

        if (event.getParticipantLimit() != 0 &&
            requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Event participant limit reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED);
        }
        request.setCreated(LocalDateTime.now());

        return requestMapper.toDto(requestRepository.save(request));
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequest(Long eventId, Long userId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);
        List<ParticipationRequest> requestList = requestRepository.findAllById(updateRequest.getRequestIds());

        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;
        if (!Objects.equals(initiatorId, userId)) {
            throw new ConflictException("Can't update event id=" + eventId + " requests by user id=" + userId);
        }
        updateRequests(requestList, toModelStatus(updateRequest.getStatus()), event);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        requestList.forEach(request -> {
            switch (request.getStatus()) {
                case RequestStatus.REJECTED -> result.getRejectedRequests().add(requestMapper.toDto(request));
                case RequestStatus.CONFIRMED -> result.getConfirmedRequests().add(requestMapper.toDto(request));
            }
        });
        return result;
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User is not the requester");
        }

        request.setStatus(RequestStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    private void updateRequests(List<ParticipationRequest> requests, RequestStatus status, EventFullDto event) {
        boolean hasNotPendingRequests = requests.stream().map(ParticipationRequest::getStatus).anyMatch(el -> el != RequestStatus.PENDING);
        if (hasNotPendingRequests)
            throw new ConflictException("Can't change status when request status is not PENDING");

        if (status == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
            }
            return;
        }
        boolean requestModeration = Boolean.TRUE.equals(event.getRequestModeration());
        Integer participantLimit = event.getParticipantLimit();
        boolean unlimited = participantLimit == null || participantLimit == 0;

        if (!requestModeration && unlimited) {
            requests.forEach(r -> r.setStatus(status));
            return;
        }

        long confirmed = requestRepository
                .countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        for (ParticipationRequest r : requests) {
            if (!unlimited && confirmed >= participantLimit) {
                throw new ConflictException("Requests out of limit");
            }
            r.setStatus(status);
            confirmed++;
        }
    }

    private static RequestStatus toModelStatus(ru.practicum.dto.request.RequestStatus s) {
        return RequestStatus.valueOf(s.name());
    }
}