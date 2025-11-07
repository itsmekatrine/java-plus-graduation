package ru.practicum.controller.publicAPI;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.parameters.PageableSearchParam;
import ru.practicum.service.CompilationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
public class CompilationController {

    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                @RequestParam(defaultValue = "10") @Positive Integer size) {
        PageableSearchParam param = PageableSearchParam.builder()
                .size(size)
                .from(from)
                .build();

        log.info("GET /compilations pinned={}, from={}, size={}", pinned, from, size);

        List<CompilationDto> all = compilationService.getAllCompilations(param.getPageable());
        if (pinned == null) return all;
        return all.stream()
                .filter(c -> Boolean.TRUE.equals(c.getPinned()) == Boolean.TRUE.equals(pinned))
                .toList();
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilationById(@PathVariable Long compId) {
        log.info("GET /compilations/{}", compId);
        return compilationService.getCompilationById(compId);
    }
}
