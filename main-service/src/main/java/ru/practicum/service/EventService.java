package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.StatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventState;
import ru.practicum.entity.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.parameters.EventAdminSearchParam;
import ru.practicum.parameters.EventUserSearchParam;
import ru.practicum.parameters.PublicSearchParam;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.practicum.specification.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.specification.EventSpecifications.eventPublicSearchParamSpec;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;

    public List<EventShortDto> getUsersEvents(EventUserSearchParam params) {
        Page<Event> events = eventRepository.findByInitiatorId(params.getUserId(), params.getPageable());

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    shortDto.setViews(views.get(event.getId()));
                    shortDto.setConfirmedRequests(confirmedRequests.get(event.getId()));
                    return shortDto;
                })
                .toList();

    }

    @Transactional
    public EventFullDto saveEvent(NewEventDto dto, Long userId) {
        Event saved = eventRepository.saveAndFlush(eventMapper.toEntity(dto, userId));
        EventFullDto fullDto = eventMapper.toFullDto(saved);
        fullDto.setViews(0L);
        fullDto.setConfirmedRequests(0L);
        return fullDto;
    }

    public List<EventShortDto> searchEvents(PublicSearchParam param) {

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(dto.getId()));
                    return dto;
                })
                .filter(Objects::nonNull);
        if (param.getSort() == SortSearchParam.VIEWS) {
            return eventShortDtoStream
                    .sorted(Comparator.comparingLong(EventShortDto::getViews))
                    .toList();
        } else {
            return eventShortDtoStream
                    .toList();
        }
    }

    public EventFullDto getEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие добавленно не теущем пользователем");
        }
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено id=" + eventId));
        if (!Objects.equals(eventToUpdate.getInitiator().getId(), userId) ||
            eventToUpdate.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Событие добавленно не теущем пользователем или уже было опубликовано");
        }
        updateNouNullFields(eventToUpdate, event);
        if (event.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (event.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }
        Event updated = eventRepository.save(eventToUpdate);

        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(eventId), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(eventId));

        EventFullDto result = eventMapper.toFullDto(updated);
        result.setConfirmedRequests(confirmed.get(eventId));
        result.setViews(views.get(eventId));
        return result;
    }

    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);
        return searched.stream()
                .limit(params.getSize())
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(event.getId()));
                    return dto;
                })
                .collect(toList());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event id=" + eventId + "not found"));
        if (event.getState() != EventState.PENDING && updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
        }
        if (event.getState() == EventState.PUBLISHED && updateRequest.getStateAction() == AdminEventAction.REJECT_EVENT) {
            throw new ConflictException("Cannot reject the event because it's not in the right state: PUBLISHED");
        }
        if (event.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
            throw new ConflictException("To late to change event");
        }
        updateNouNullFields(event, updateRequest);
        event.setState(updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT ? EventState.PUBLISHED : EventState.CANCELED);
        Event updated = eventRepository.save(event);

        EventFullDto dto = eventMapper.toFullDto(updated);

        Map<Long, Long> views = getViews(List.of(eventId));
        Map<Long, Long> confirmedRequests = requestRepository.countRequestsByEventIdsAndStatus(List.of(eventId),
                RequestStatus.CONFIRMED);

        dto.setViews(views.get(eventId));
        dto.setConfirmedRequests(confirmedRequests.get(eventId));

        return dto;
    }

    private void updateNouNullFields(Event eventToUpdate, UpdateEventRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            eventToUpdate.setLat(event.getLocation().getLat());
            eventToUpdate.setLon(event.getLocation().getLon());
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    /**
     * Getting stats from stats client
     */
    private Map<Long, Long> getViews(List<Long> eventIds) {
        List<StatsDto> stats = statsClient.getStats(
                "2000-01-01 00:00:00",
                "2100-01-01 00:00:00",
                eventIds.stream().map(id -> "/events/" + id).toList(),
                true);
        return stats.stream()
                .filter(statsDto -> !statsDto.getUri().equals("/events"))
                .collect(toMap(statDto ->
                        Long.parseLong(statDto.getUri().replace("/events/", "")), StatsDto::getHits));
    }
}
