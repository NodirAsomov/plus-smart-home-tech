package ru.yandex.practicum.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.repository.OrderRepository;

@Service
public class OrderPersistenceService {
    private final OrderRepository repository;
    public OrderPersistenceService(OrderRepository repository) { this.repository = repository; }
    @Transactional public OrderDto save(Order order) { return OrderService.toDto(repository.saveAndFlush(order)); }
}
