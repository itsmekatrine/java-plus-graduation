package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.HitDto;
import ru.practicum.dto.StatsDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {
    @PostMapping("/hit")
    void postHit(@RequestBody HitDto dto);

    @GetMapping("/stats")
    List<StatsDto> getStats(@RequestParam String start,
                            @RequestParam String end,
                            @RequestParam(required = false) List<String> uris,
                            @RequestParam(defaultValue = "false") boolean unique);
}