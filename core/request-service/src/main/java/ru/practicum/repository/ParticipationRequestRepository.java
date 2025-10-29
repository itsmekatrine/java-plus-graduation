package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @Query("""
        select r.eventId as eventId, count(r.id) as cnt
        from ParticipationRequest r
        where r.eventId in ?1 and r.status = ?2
        group by r.eventId
    """)
    List<EventCountRow> countRequestsByStatus(List<Long> eventIds, RequestStatus status);

    default Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> eventIds, RequestStatus status) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();
        return countRequestsByStatus(eventIds, status).stream()
                .collect(Collectors.toMap(EventCountRow::getEventId, EventCountRow::getCnt));
    }

    interface EventCountRow {
        Long getEventId();
        Long getCnt();
    }

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatus(Long id, Long id1, RequestStatus status);
}