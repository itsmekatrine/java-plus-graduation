package ru.practicum.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class EventSimilarityConsumer extends KafkaConsumer<String, EventSimilarityAvro> {
    public EventSimilarityConsumer(AnalyzerKafkaConfig config) {
        super(AnalyzerKafkaProperties.mapToProperties(config.getConsumer().getSimilarity().getProperties()));
    }
}
