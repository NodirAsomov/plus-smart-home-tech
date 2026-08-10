package ru.yandex.practicum.product.controller;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.*;
import ru.yandex.practicum.product.service.CategoryService;
import java.util.List;
@RestController @RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @GetMapping public List<CategoryDto> all() { return service.findAll(); }
    @GetMapping("/{id}") public CategoryDto byId(@PathVariable Long id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CategoryDto create(@Valid @RequestBody CreateCategoryRequest request) { return service.create(request); }
}
