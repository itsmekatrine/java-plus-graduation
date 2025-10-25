package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.entity.Category;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    @Transactional
    public CategoryDto save(NewCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ConflictException("Category name is already in use");
        }
        Category saved = categoryRepository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    @Transactional
    public CategoryDto update(CategoryDto dto) {
        Category category = categoryRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getId() + " was not found"));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), dto.getId())) {
            throw new ConflictException("Category name is already in use");
        }

        category.setName(dto.getName());
        categoryRepository.saveAndFlush(category);
        return mapper.toDto(category);
    }

    public List<CategoryDto> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).stream()
                .map(mapper::toDto)
                .toList();
    }

    public CategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));
    }
}
