package ru.yandex.practicum.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.*;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.repository.OrderRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) { this.repository = repository; }
    @Transactional public OrderDto create(CreateOrderRequest request) {
        Order order = new Order(request.customerName(), request.customerEmail());
        request.items().forEach(i -> order.addItem(new OrderItem(i.productId(), i.productName(), i.quantity(), i.price())));
        return toDto(repository.save(order));
    }
    public OrderDto findById(Long id) { return toDto(repository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id))); }
    public List<OrderDto> findAll() { return repository.findAll().stream().map(this::toDto).toList(); }
    public List<OrderDto> findByEmail(String email) { return repository.findByCustomerEmailOrderByCreatedAtDesc(email).stream().map(this::toDto).toList(); }
    private OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(i -> new OrderItemDto(i.getId(), i.getProductId(), i.getProductName(), i.getQuantity(), i.getPrice())).toList();
        return new OrderDto(o.getId(), o.getCustomerName(), o.getCustomerEmail(), o.getStatus(), o.getTotalPrice(), o.getStatusDetails(), o.getCreatedAt(), items);
    }
}
