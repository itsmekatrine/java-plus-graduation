package ru.practicum.kafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyzerKafkaProperties {
    public static Properties mapToProperties(Map<String, String> configMap) {
        Properties properties = new Properties();
        if (configMap == null || configMap.isEmpty()) {
            return properties;
        }
        configMap.forEach((k, v) -> {
            String key = k.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('-', '.')
                    .replace('_', '.');
            properties.setProperty(key, String.valueOf(v));
        });
        return properties;
    }
}
