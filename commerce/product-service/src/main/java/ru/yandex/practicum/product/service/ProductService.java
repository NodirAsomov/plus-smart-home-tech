package ru.yandex.practicum.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.*;
import ru.yandex.practicum.product.entity.*;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.ProductRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    private final CategoryService categories;
    public ProductService(ProductRepository repository, CategoryService categories) { this.repository = repository; this.categories = categories; }
    @Transactional public ProductDto create(CreateProductRequest r) {
        Category category = r.categoryId() == null ? null : categories.get(r.categoryId());
        return toDto(repository.save(new Product(r.name(), r.description(), r.price(), category, r.imageUrl())));
    }
    public ProductDto findById(Long id) { return toDto(get(id)); }
    public List<ProductDto> findAll(Long categoryId) {
        List<Product> products = categoryId == null ? repository.findByActiveTrue() : repository.findByCategoryIdAndActiveTrue(categoryId);
        return products.stream().map(this::toDto).toList();
    }
    public List<ProductDto> search(String query) { return repository.findByNameContainingIgnoreCaseAndActiveTrue(query).stream().map(this::toDto).toList(); }
    @Transactional public ProductDto update(Long id, UpdateProductRequest r) {
        Product product = get(id);
        Category category = r.categoryId() == null ? null : categories.get(r.categoryId());
        product.update(r.name(), r.description(), r.price(), category, r.imageUrl(), r.active());
        return toDto(product);
    }
    private Product get(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id)); }
    private ProductDto toDto(Product p) {
        CategoryDto category = p.getCategory() == null ? null : categories.toDto(p.getCategory());
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), category, p.getImageUrl(), p.isActive());
    }
}
