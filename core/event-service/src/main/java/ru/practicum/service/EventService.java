package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.*;
import ru.practicum.dto.stats.StatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.RequestClient;
import ru.practicum.feign.StatsClient;
import ru.practicum.mapper.EventMapper;
import ru.practicum.parameters.EventAdminSearchParam;
import ru.practicum.parameters.EventUserSearchParam;
import ru.practicum.parameters.PublicSearchParam;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static ru.practicum.specification.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.specification.EventSpecifications.eventPublicSearchParamSpec;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;

    public List<EventShortDto> getUsersEvents(EventUserSearchParam params) {
        Page<Event> events = eventRepository.findByInitiatorId(params.getUserId(), params.getPageable());

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = eventIds.isEmpty()
                ? Collections.emptyMap()
                : requestClient.countByEvent(eventIds, "CONFIRMED");

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    final Long eventId = event.getId();
                    dto.setViews(views.getOrDefault(eventId, 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(eventId, 0L));
                    return dto;
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

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = getConfirmedMap(eventIds);

        boolean onlyAvailable = Boolean.TRUE.equals(param.getOnlyAvailable());
        boolean sortByViews = SortSearchParam.VIEWS.equals(param.getSort());

        Stream<EventShortDto> stream = events.stream()
                .map(event -> {
                    Long eventId = event.getId();
                    long confirmedCnt = confirmed.getOrDefault(eventId, 0L);
                    Integer limit = event.getParticipantLimit();

                    if (onlyAvailable && limit != null && limit > 0 && confirmedCnt >= limit) {
                        return null;
                    }

                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmedCnt);
                    dto.setViews(views.getOrDefault(eventId, 0L));
                    return dto;
                })
                .filter(Objects::nonNull);

        if (sortByViews) {
            return stream.sorted(Comparator.comparingLong(EventShortDto::getViews).reversed())
                    .toList();
        }

        return stream.toList();
    }

    public EventFullDto getEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));

        Long eventId = event.getId();

        Map<Long, Long> confirmed = getConfirmedMap(List.of(eventId));
        Map<Long, Long> views = getViews(List.of(eventId));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.getOrDefault(eventId, 0L));
        dto.setViews(views.getOrDefault(eventId, 0L));
        return dto;
    }

    public EventFullDto getEventForInternalUse(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        Long id = event.getId();
        Map<Long, Long> confirmed = getConfirmedMap(List.of(id));
        Map<Long, Long> views = getViews(List.of(id));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.getOrDefault(id, 0L));
        dto.setViews(views.getOrDefault(id, 0L));
        return dto;
    }

    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ConflictException("Событие добавлено не текущим пользователем");
        }

        Long id = event.getId();
        Map<Long, Long> confirmed = getConfirmedMap(List.of(id));
        Map<Long, Long> views = getViews(List.of(id));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.getOrDefault(id, 0L));
        dto.setViews(views.getOrDefault(id, 0L));
        return dto;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено id=" + eventId));
        if (!Objects.equals(eventToUpdate.getInitiatorId(), userId) ||
                eventToUpdate.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Событие добавлено не текущим пользователем или уже было опубликовано");
        }

        updateNouNullFields(eventToUpdate, event);
        if (event.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (event.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }

        Event updated = eventRepository.save(eventToUpdate);
        Long id = updated.getId();

        Map<Long, Long> confirmed = getConfirmedMap(List.of(id));
        Map<Long, Long> views = getViews(List.of(id));

        EventFullDto result = eventMapper.toFullDto(updated);
        result.setConfirmedRequests(confirmed.getOrDefault(id, 0L));
        result.setViews(views.getOrDefault(id, 0L));
        return result;
    }

    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = getConfirmedMap(eventIds);

        return searched.stream()
                .limit(params.getSize())
                .map(event -> {
                    Long id = event.getId();
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmed.getOrDefault(id, 0L));
                    dto.setViews(views.getOrDefault(id, 0L));
                    return dto;
                })
                .collect(toList());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event id=" + eventId + " not found"));

        AdminEventAction action = updateRequest.getStateAction();

        updateNouNullFields(event, updateRequest);

        if (action == AdminEventAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = event.getEventDate();
            if (start == null || !start.isAfter(now.plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour after publish time");
            }

            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(now);

        } else if (action == AdminEventAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's already PUBLISHED");
            }

            event.setState(EventState.CANCELED);
            event.setPublishedOn(null);
        }

        Event saved = eventRepository.save(event);

        EventFullDto dto = eventMapper.toFullDto(saved);
        Long id = saved.getId();
        Map<Long, Long> views = getViews(List.of(id));
        Map<Long, Long> confirmed = getConfirmedMap(List.of(id));
        dto.setViews(views.getOrDefault(id, 0L));
        dto.setConfirmedRequests(confirmed.getOrDefault(id, 0L));
        return dto;
    }

    public EventRequestStatusUpdateResult updateEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long initiatorId = event.getInitiatorId();
        if (initiatorId == null || !initiatorId.equals(userId)) {
            throw new ConflictException("Only the initiator can change request statuses");
        }
        return requestClient.updateEventRequests(userId, eventId, body);
    }

    @Transactional
    public void appendForbiddenWords(Long eventId, Set<String> words) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (e.getForbiddenWords() == null) {
            e.setForbiddenWords(new HashSet<>());
        }
        if (words != null && !words.isEmpty()) {
            e.getForbiddenWords().addAll(words);
        }
        eventRepository.save(e);
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
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2100, 1, 1, 0, 0, 0);

        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();
        List<StatsDto> stats = statsClient.getStats(start, end, uris, true);
        if (stats == null || stats.isEmpty()) return Collections.emptyMap();

        return stats.stream()
                .filter(s -> s.getUri() != null && s.getUri().startsWith("/events/"))
                .collect(Collectors.toMap(
                        s -> Long.parseLong(s.getUri().substring("/events/".length())),
                        StatsDto::getHits,
                        Long::sum
                ));
    }

    private Map<Long, Long> getConfirmedMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> map = requestClient.countByEvent(eventIds, "CONFIRMED");
        return map != null ? map : Collections.emptyMap();
    }
}
