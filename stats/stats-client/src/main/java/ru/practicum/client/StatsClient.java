package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.HitDto;
import ru.practicum.dto.StatsDto;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;

    @Value("${stats.server.url:http://localhost:9090}")
    private String url;

    public void postHit(HitDto dto) {
        restTemplate.postForEntity(url + "/hit", dto, Void.class);
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique) throws RestClientException {

        StringBuilder sb = new StringBuilder(url).append("/stats")
                .append("?start=").append(start)
                .append("&end=").append(end)
                .append("&unique=").append(unique);

        if (uris != null) {
            for (String uri : uris) {
                sb.append("&uris=").append(uri);
            }
        }

        ResponseEntity<StatsDto[]> response = restTemplate.getForEntity(sb.toString(), StatsDto[].class);

        StatsDto[] body = response.getBody();
        return (body == null) ? Collections.emptyList() : List.of(body);
    }
}