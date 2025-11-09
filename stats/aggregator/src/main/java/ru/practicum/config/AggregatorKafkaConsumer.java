package ru.practicum.config;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AggregatorKafkaConsumer extends KafkaConsumer<Long, UserActionAvro> {
    public AggregatorKafkaConsumer(AggregatorKafkaConfig config) {
        super(AggregatorKafkaProperties.mapToProperties(config.getConsumer().getProperties()));
    }
}
