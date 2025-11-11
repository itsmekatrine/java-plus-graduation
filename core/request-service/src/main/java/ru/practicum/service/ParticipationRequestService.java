package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.CollectorClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestMapper requestMapper;
    private final CollectorClient collectorClient;

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

    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        Optional<ParticipationRequest> existingOpt =
                requestRepository.findByRequesterIdAndEventId(userId, eventId);

        EventFullDto event = eventClient.findEventForInternalUse(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event must be PUBLISHED");
        }

        Long initiatorId = (event.getInitiator() != null)
                ? event.getInitiator().getId()
                : null;

        if (initiatorId != null && initiatorId.equals(userId)) {
            throw new ConflictException("Initiator cannot request participation for own event");
        }

        Integer limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        boolean autoConfirm = Boolean.FALSE.equals(event.getRequestModeration()) || limit == 0;

        if (existingOpt.isPresent()) {
            ParticipationRequest existing = existingOpt.get();
            if (existing.getStatus() == RequestStatus.PENDING || existing.getStatus() == RequestStatus.CONFIRMED) {
                throw new ConflictException("Participation request already exists");
            }
            if (existing.getStatus() == RequestStatus.CANCELED /* || existing.getStatus() == RequestStatus.REJECTED */) {
                existing.setCreated(LocalDateTime.now());
                existing.setStatus(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING);

                try {
                    collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
                }
                catch (Exception ignore) {
                }

                return requestMapper.toDto(requestRepository.save(existing));
            }
            throw new ConflictException("Participation request already exists");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();

        try {
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
        }
        catch (Exception ignore) {
        }
        return requestMapper.toDto(requestRepository.save(request));
    }

    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Some participation requests were not found by ids");
        }

        for (ParticipationRequest r : requests) {
            if (!r.getEventId().equals(eventId)) {
                throw new NotFoundException("Request id=" + r.getId() + " does not match eventId=" + eventId);
            }
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Only requests in PENDING status can be updated");
            }
        }

        Integer limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        int confirmedBefore = requestRepository
                .findAllByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                .size();

        boolean confirmMode = updateRequest.getStatus().equals(RequestStatus.CONFIRMED);
        int toConfirm = confirmMode ? requests.size() : 0;

        if (limit > 0 && confirmedBefore + toConfirm > limit) {
            throw new ConflictException("Event limit exceed");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (confirmMode) {
            for (ParticipationRequest r : requests) {
                r.setStatus(RequestStatus.CONFIRMED);
            }
            requestRepository.saveAllAndFlush(requests);
            requests.forEach(r -> confirmedRequests.add(requestMapper.toDto(r)));

            if (limit > 0 && confirmedBefore + toConfirm == limit) {
                List<ParticipationRequest> pendingOthers =
                        requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
                if (!pendingOthers.isEmpty()) {
                    pendingOthers.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                    requestRepository.saveAllAndFlush(pendingOthers);
                    pendingOthers.forEach(r -> rejectedRequests.add(requestMapper.toDto(r)));
                }
            }
        } else if (updateRequest.getStatus().equals(RequestStatus.REJECTED)) {
            for (ParticipationRequest r : requests) {
                r.setStatus(RequestStatus.REJECTED);
            }
            requestRepository.saveAllAndFlush(requests);
            requests.forEach(r -> rejectedRequests.add(requestMapper.toDto(r)));
        } else {
            throw new ConflictException("Unknown target status: " + updateRequest.getStatus());
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest r = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!Objects.equals(r.getRequesterId(), userId)) {
            throw new ConflictException("User is not the requester");
        }
        if (r.getStatus() == RequestStatus.CONFIRMED) {
            throw new ConflictException("Cannot cancel a confirmed request");
        }
        r.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.saveAndFlush(r);
        return requestMapper.toDto(saved);
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
        requestRepository.saveAllAndFlush(pendings);
        pendings.forEach(p -> out.getRejectedRequests().add(requestMapper.toDto(p)));
    }
}