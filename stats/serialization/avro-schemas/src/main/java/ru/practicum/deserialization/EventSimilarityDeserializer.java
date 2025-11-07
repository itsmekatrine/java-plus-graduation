package ru.practicum.deserialization;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public EventSimilarityDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
