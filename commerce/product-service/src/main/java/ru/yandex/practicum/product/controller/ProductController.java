package ru.yandex.practicum.product.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.*;
import ru.yandex.practicum.product.service.ProductService;
import java.util.List;
@RestController @RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping public List<ProductDto> all(@RequestParam(required = false) Long categoryId) { return service.findAll(categoryId); }
    @GetMapping("/search") public List<ProductDto> search(@RequestParam String query) { return service.search(query); }
    @GetMapping("/category/{categoryId}") public List<ProductDto> byCategory(@PathVariable Long categoryId) { return service.findAll(categoryId); }
    @GetMapping("/{id}") public ProductDto byId(@PathVariable Long id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProductDto create(@Valid @RequestBody CreateProductRequest request) { return service.create(request); }
    @PatchMapping("/{id}") public ProductDto update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) { return service.update(id, request); }
}
