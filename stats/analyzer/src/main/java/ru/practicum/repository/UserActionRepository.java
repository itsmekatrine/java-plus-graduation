package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserAction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByUserId(Long userId, Pageable pageable);

    List<UserAction> findAllByEventIdInAndUserId(Collection<Long> eventIds, Long userId);

    List<UserAction> findAllByEventIdIn(Collection<Long> eventIds);

    @Query("SELECT ua.eventId FROM UserAction ua WHERE ua.userId = :userId AND ua.eventId IN :eventIds")
    Set<Long> findEventIdsByUserIdAndEventIdIn(@Param("userId") Long userId,
                                               @Param("eventIds") Collection<Long> eventIds);
}
