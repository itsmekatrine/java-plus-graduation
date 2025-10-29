package ru.practicum.service;

import ru.practicum.dto.stats.HitDto;
import ru.practicum.dto.stats.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveHit(HitDto hitDto);

    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
