package ru.practicum.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.kafka")
public class AnalyzerKafkaConfig {
    private final String bootstrapServers;
    private final ConsumerProperties consumer;

    @Getter
    @RequiredArgsConstructor
    public static class ConsumerProperties {
        private final Consumer similarity;
        private final Consumer userAction;

        @Getter
        @RequiredArgsConstructor
        public static class Consumer {
            private final Map<String, String> properties;
            private final List<String> topics;
            private final Duration pollTimeout;
        }
    }
}

