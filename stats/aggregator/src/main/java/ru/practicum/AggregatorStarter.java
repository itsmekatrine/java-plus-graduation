package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.config.AggregatorKafkaConsumer;
import ru.practicum.config.AggregatorKafkaProducer;
import ru.practicum.config.AggregatorKafkaConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.UserActionService;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorStarter implements CommandLineRunner {
    private final AggregatorKafkaConfig kafkaConfig;
    private final AggregatorKafkaProducer producer;
    private final AggregatorKafkaConsumer consumer;
    private final UserActionService userActionService;

    @Override
    public void run(String... args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(kafkaConfig.getConsumer().getTopics());
            Duration pollTimeout = kafkaConfig.getConsumer().getPollTimeout();

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(pollTimeout);
                if (!records.isEmpty()) {
                    log.info("Received {} records: {}", records.count(), records);
                    for (ConsumerRecord<Long, UserActionAvro> record : records) {
                        UserActionAvro action = record.value();
                        log.info("User action handling: {}", action);
                        userActionService.handle(action)
                                .forEach(producer::send);
                        log.info("User action handled");
                    }
                    consumer.commitAsync();
                    log.info("Offset committed");
                }
            }
        } catch (WakeupException e) {
            log.error("WakeupException", e);
        } catch (Exception e) {
            log.error("User action handling error", e);
        } finally {
            try {
                producer.flush();
                consumer.commitAsync();
                log.info("Refreshed");
            } catch (Exception e) {
                log.error("Refreshing error", e);
            } finally {
                consumer.close();
                log.info("Consumer closed");
                producer.close();
                log.info("Producer closed");
            }
        }
    }
}
