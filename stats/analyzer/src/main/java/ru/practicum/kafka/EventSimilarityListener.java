package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.handler.EventSimilarityHandler;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityListener implements Runnable {
    private final EventSimilarityConsumer consumer;
    private final AnalyzerKafkaConfig kafkaConfig;
    private final EventSimilarityHandler eventSimilarityHandler;

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(kafkaConfig.getConsumer().getSimilarity().getTopics());
            Duration pollTimeout = kafkaConfig.getConsumer().getSimilarity().getPollTimeout();

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(pollTimeout);

                if (!records.isEmpty()) {
                    log.info("Received {} records: {}", records.count(), records);

                    for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                        EventSimilarityAvro eventSimilarityAvro = record.value();
                        log.info("Event similarity handling: {}", eventSimilarityAvro);
                        eventSimilarityHandler.handle(eventSimilarityAvro);
                        log.info("Event similarity handled");
                    }

                    consumer.commitAsync();
                    log.info("Offset committed");
                }
            }
        } catch (WakeupException e) {
            log.error("WakeupException", e);
        } catch (Exception e) {
            log.error("Event similarity handling error", e);
        } finally {
            try {
                consumer.commitAsync();
                log.info("Refreshed");
            } catch (Exception e) {
                log.error("Refreshing error", e);
            } finally {
                consumer.close();
                log.info("Consumer closed");
            }
        }
    }
}