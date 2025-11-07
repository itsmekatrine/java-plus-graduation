package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.entity.Compilation;
import ru.practicum.entity.Event;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<CompilationDto> getAllCompilations(Pageable pageable) {
        return compilationRepository.findAll(pageable).stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .map(compilationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        Set<Event> events = Collections.emptySet();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Long> ids = dto.getEvents().stream()
                    .mapToLong(n -> ((Number) n).longValue())
                    .boxed()
                    .distinct()
                    .toList();

            List<Event> found = eventRepository.findAllById(ids);

            if (found.size() != ids.size()) {
                Set<Long> foundIds = found.stream().map(Event::getId).collect(java.util.stream.Collectors.toSet());
                List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
                throw new NotFoundException("Events not found: " + missing);
            }

            events = new LinkedHashSet<>(found);
        }

        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(Boolean.TRUE.equals(dto.getPinned()))
                .events(events)
                .build();

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        compilationRepository.deleteById(compId);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }
}
