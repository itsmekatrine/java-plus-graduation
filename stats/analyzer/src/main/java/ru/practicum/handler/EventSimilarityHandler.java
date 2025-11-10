package ru.practicum.handler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class EventSimilarityHandler {
    private final EventSimilarityRepository eventSimilarityRepository;

    public void handle(EventSimilarityAvro avro) {
        EventSimilarity eventSimilarity = createEventSimilarity(avro);
        eventSimilarityRepository.save(eventSimilarity);
        log.debug("Saved event similarity: {}", eventSimilarity);
    }

    private EventSimilarity createEventSimilarity(EventSimilarityAvro avro) {
        EventSimilarity eventSimilarity = new EventSimilarity();
        eventSimilarity.setEventA(avro.getEventA());
        eventSimilarity.setEventB(avro.getEventB());
        eventSimilarity.setScore(avro.getScore());
        eventSimilarity.setTimestamp(avro.getTimestamp());
        return eventSimilarity;
    }
}