package ru.yandex.practicum.inventory.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.*;
import ru.yandex.practicum.inventory.service.InventoryService;
import java.util.List;
@RestController @RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }
    @GetMapping public List<InventoryDto> all() { return service.findAll(); }
    @GetMapping("/{productId}") public InventoryDto byProduct(@PathVariable Long productId) { return service.findByProductId(productId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public InventoryDto create(@Valid @RequestBody UpdateInventoryRequest request) { return service.create(request); }
    @PutMapping public InventoryDto update(@Valid @RequestBody UpdateInventoryRequest request) { return service.update(request); }
    @PostMapping("/reserve") public ReserveResponse reserve(@Valid @RequestBody ReserveRequest request) { return service.reserve(request); }
    @PostMapping("/release") public ReserveResponse release(@Valid @RequestBody ReserveRequest request) { return service.release(request); }
}
