package ru.yandex.practicum.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.*;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.*;
import ru.yandex.practicum.inventory.repository.InventoryRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    private final InventoryRepository repository;
    public InventoryService(InventoryRepository repository) { this.repository = repository; }
    public List<InventoryDto> findAll() { return repository.findAll().stream().map(this::toDto).toList(); }
    public InventoryDto findByProductId(Long productId) { return toDto(get(productId)); }
    @Transactional public InventoryDto create(UpdateInventoryRequest request) {
        if (repository.existsByProductId(request.productId())) throw new ConflictException("Inventory record already exists for product: " + request.productId());
        return toDto(repository.save(new Inventory(request.productId(), request.quantity())));
    }
    @Transactional public InventoryDto update(UpdateInventoryRequest request) {
        Inventory inventory = get(request.productId());
        inventory.setQuantity(request.quantity());
        return toDto(inventory);
    }
    @Transactional public ReserveResponse reserve(ReserveRequest request) {
        Inventory inventory = get(request.productId());
        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + request.productId());
        }
        inventory.reserve(request.quantity());
        repository.flush();
        return new ReserveResponse(true, inventory.getAvailableQuantity(), "Product reserved successfully");
    }
    @Transactional public ReserveResponse release(ReserveRequest request) {
        Inventory inventory = get(request.productId());
        inventory.release(request.quantity());
        repository.flush();
        return new ReserveResponse(true, inventory.getAvailableQuantity(), "Product reservation released successfully");
    }
    private Inventory get(Long productId) { return repository.findByProductId(productId).orElseThrow(() -> new NotFoundException("Inventory not found for product: " + productId)); }
    private InventoryDto toDto(Inventory i) { return new InventoryDto(i.getId(), i.getProductId(), i.getQuantity(), i.getReservedQuantity(), i.getAvailableQuantity()); }
}
