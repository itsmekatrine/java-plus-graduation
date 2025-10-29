package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @Query("""
            select r.event.id, count(r.id)
            from ParticipationRequest r
            where r.event.id in ?1 and r.status = ?2
            group by r.event.id""")
    List<Object[]> countRequestsByStatus(List<Long> ids, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    default Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> ids, RequestStatus status) {
        List<Object[]> result = countRequestsByStatus(ids, status);
        return result.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    List<ParticipationRequest> findAllByEventId(Long eventId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    boolean existsByRequesterIdAndEventIdAndStatus(Long id, Long id1, RequestStatus status);
}