package ru.yandex.practicum.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.*;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.CategoryRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    public CategoryService(CategoryRepository repository) { this.repository = repository; }
    @Transactional public CategoryDto create(CreateCategoryRequest request) { return toDto(repository.save(new Category(request.name(), request.description()))); }
    public List<CategoryDto> findAll() { return repository.findAll().stream().map(this::toDto).toList(); }
    public CategoryDto findById(Long id) { return toDto(get(id)); }
    Category get(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Category not found: " + id)); }
    CategoryDto toDto(Category c) { return new CategoryDto(c.getId(), c.getName(), c.getDescription()); }
}
