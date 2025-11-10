package ru.practicum.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaConfig {
    private final String bootstrapServers;
    private final Producer producer;
    private final Consumer consumer;

    @Getter
    @RequiredArgsConstructor
    public static class Producer {
        private final Map<String, String> properties;
        private final String topic;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Consumer {
        private final Map<String, String> properties;
        private final List<String> topics;
        private final Duration pollTimeout;
    }
}