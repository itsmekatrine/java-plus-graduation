package ru.practicum.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AggregatorKafkaProperties {
    public static Properties mapToProperties(Map<String, String> configMap) {
        Properties properties = new Properties();
        properties.putAll(configMap);
        return properties;
    }
}
