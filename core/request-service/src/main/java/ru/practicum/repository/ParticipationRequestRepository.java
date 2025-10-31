package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.ParticipationRequest;

import java.util.Collection;
import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @Query("""
        select r.eventId as eventId, count(r.id) as cnt
        from ParticipationRequest r
        where r.eventId in :eventIds and r.status = :status
        group by r.eventId
    """)
    List<EventCountRow> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                                 @Param("status") RequestStatus status);

    interface EventCountRow {
        Long getEventId();
        Long getCnt();
    }

    @Query("""
        select count(r) from ParticipationRequest r
        where r.status = 'CONFIRMED' and r.eventId = :eventId
    """)
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByEventIdInAndStatus(Collection<Long> eventIds, RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByIdIn(List<Long> requestIds);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);
}