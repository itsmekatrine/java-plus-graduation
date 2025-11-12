package ru.practicum.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionConsumer extends KafkaConsumer<Long, UserActionAvro> {
    public UserActionConsumer(AnalyzerKafkaConfig config) {
        super(AnalyzerKafkaProperties.mapToProperties(config.getConsumer().getUserAction().getProperties()));
    }
}
