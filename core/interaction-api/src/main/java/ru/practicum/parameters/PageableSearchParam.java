package ru.practicum.parameters;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageableSearchParam {

    private Integer from = 0;
    private Integer size = 10;

    public Pageable getPageable() {
        int s = (size == null || size <= 0) ? 10 : size;
        int f = (from == null || from < 0) ? 0 : from;
        int page = f / s;
        return PageRequest.of(page, s);
    }

    public Pageable getPageable(Sort sort) {
        int s = (size == null || size <= 0) ? 10 : size;
        int f = (from == null || from < 0) ? 0 : from;
        int page = f / s;
        return PageRequest.of(page, s, (sort == null ? Sort.unsorted() : sort));
    }
}
