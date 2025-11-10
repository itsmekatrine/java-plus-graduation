package ru.practicum.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class AggregatorKafkaProducer extends KafkaProducer<String, SpecificRecordBase> {
    private final String topic;

    public AggregatorKafkaProducer(AggregatorKafkaConfig config) {
        super(AggregatorKafkaProperties.mapToProperties(config.getProducer().getProperties()));
        this.topic = config.getProducer().getTopic();
    }

    public void send(EventSimilarityAvro similarity) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                similarity.getTimestamp().toEpochMilli(),
                "%s_%s".formatted(similarity.getEventA(), similarity.getEventB()),
                similarity
        );
        super.send(record);
    }

    @Override
    public void close() {
        flush();
        super.close();
    }
}