package ru.practicum.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.IntegrationTestBase;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.entity.Compilation;
import ru.practicum.exception.NotFoundException;
import ru.practicum.repository.CompilationRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CompilationServiceIntegrationTest extends IntegrationTestBase {

    private final CompilationService compilationService;
    private final CompilationRepository compilationRepository;
    private final EntityManager entityManager;

    @Test
    void getAllCompilations_shouldReturnAll() {
        Compilation compilation1 = new Compilation();
        compilation1.setTitle("Favorites");
        compilation1.setPinned(true);
        entityManager.persist(compilation1);

        Compilation compilation2 = new Compilation();
        compilation2.setTitle("New Hits");
        compilation2.setPinned(false);
        entityManager.persist(compilation2);

        entityManager.flush();
        entityManager.clear();

        List<CompilationDto> result = compilationService.getAllCompilations(PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CompilationDto::getTitle)
                .containsExactlyInAnyOrder("Favorites", "New Hits");
    }

    @Test
    void getCompilationById_shouldReturnCompilation() {
        Compilation compilation = new Compilation();
        compilation.setTitle("Cinema");
        compilation.setPinned(true);
        entityManager.persist(compilation);
        entityManager.flush();
        entityManager.clear();

        CompilationDto dto = compilationService.getCompilationById(compilation.getId());

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("Cinema");
        assertThat(dto.getPinned()).isTrue();
    }

    @Test
    void getCompilationById_shouldThrowIfNotFound() {
        assertThatThrownBy(() -> compilationService.getCompilationById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка не найдена");
    }
}