package ru.practicum.parameters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@AllArgsConstructor
@Builder
@Getter
public class EventUserSearchParam {
    private Long userId;
    private Integer from;
    private Integer size;

    public Pageable getPageable() {
        int f = (from == null || from < 0) ? 0 : from;
        int s = (size == null || size <= 0) ? 10 : size;
        int page = f / s;
        return PageRequest.of(page, s);
    }
}
