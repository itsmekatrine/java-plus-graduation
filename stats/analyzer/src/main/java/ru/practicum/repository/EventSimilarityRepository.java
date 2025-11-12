package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.EventSimilarity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    List<EventSimilarity> findAllByEventAIn(Collection<Long> eventAIds, Pageable pageable);

    List<EventSimilarity> findAllByEventBIn(Collection<Long> eventBIds, Pageable pageable);

    List<EventSimilarity> findAllByEventAIn(Set<Long> candidateEventIds);

    List<EventSimilarity> findAllByEventBIn(Set<Long> candidateEventIds);

    List<EventSimilarity> findAllByEventA(Long eventA, Pageable pageable);

    List<EventSimilarity> findAllByEventB(Long eventB, Pageable pageable);
}
