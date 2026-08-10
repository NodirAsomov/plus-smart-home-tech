package ru.yandex.practicum.order.controller;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.service.OrderService;
import java.util.List;
@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderDto create(@Valid @RequestBody CreateOrderRequest request) { return service.create(request); }
    @GetMapping public List<OrderDto> all() { return service.findAll(); }
    @GetMapping("/by-email") public List<OrderDto> byEmail(@RequestParam String email) { return service.findByEmail(email); }
    @GetMapping("/{id}") public OrderDto byId(@PathVariable Long id) { return service.findById(id); }
}
