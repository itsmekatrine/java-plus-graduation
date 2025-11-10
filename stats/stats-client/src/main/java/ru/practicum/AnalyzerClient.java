package ru.practicum;

import java.util.Collection;
import java.util.Map;

public interface AnalyzerClient {
    Map<Long, Double> getRatings(Collection<Long> eventIds);
}