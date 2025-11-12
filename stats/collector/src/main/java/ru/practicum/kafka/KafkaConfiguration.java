package ru.practicum.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.serialization.GeneralAvroSerializer;

import java.util.Properties;

@Configuration
public class KafkaConfiguration {
    private final String kafkaServer;
    private final String userActionTopic;

    public KafkaConfiguration(@Value("${app.kafka.server}") String kafkaServer,
                              @Value("${app.kafka.user-action-topic}") String userActionTopic) {
        this.kafkaServer = kafkaServer;
        this.userActionTopic = userActionTopic;
    }

    @Bean
    KafkaClient getClient() {
        return new KafkaClient() {

            private Producer<Long, SpecificRecordBase> producer;

            @Override
            public Producer<Long, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    initProducer();
                }
                return producer;
            }

            private void initProducer() {
                Properties config = new Properties();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);

                producer = new KafkaProducer<>(config);
            }

            @Override
            @PreDestroy
            public void stop() {
                if (producer != null) {
                    producer.flush();
                    producer.close();
                }
            }

            @Override
            public String getUserActionTopic() {
                return userActionTopic;
            }
        };
    }
}
