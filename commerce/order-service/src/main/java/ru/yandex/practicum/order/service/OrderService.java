package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.dto.InventoryRequest;
import ru.yandex.practicum.order.client.dto.ProductResponse;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.repository.OrderRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderPersistenceService persistenceService;

    public OrderService(OrderRepository repository, ProductClient productClient,
                        InventoryClient inventoryClient, OrderPersistenceService persistenceService) {
        this.repository = repository; this.productClient = productClient;
        this.inventoryClient = inventoryClient; this.persistenceService = persistenceService;
    }

    /** Remote calls intentionally execute without an order database transaction. */
    public OrderDto create(CreateOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(item -> quantities.merge(item.productId(), item.quantity(), Integer::sum));
        Map<Long, ProductResponse> products = new LinkedHashMap<>();
        for (Long productId : quantities.keySet()) {
            ProductResponse product = loadProduct(productId);
            if (!Boolean.TRUE.equals(product.active()))
                throw new OrderProcessingException("Product " + productId + " is not available for sale");
            products.put(productId, product);
        }

        Map<Long, Integer> reserved = new LinkedHashMap<>();
        try {
            for (var entry : quantities.entrySet()) {
                reserve(entry.getKey(), entry.getValue());
                reserved.put(entry.getKey(), entry.getValue());
            }
            Order order = new Order(request.customerName(), request.customerEmail());
            for (OrderItemRequest item : request.items()) {
                ProductResponse product = products.get(item.productId());
                order.addItem(new OrderItem(product.id(), product.name(), item.quantity(), product.price()));
            }
            return persistenceService.save(order);
        } catch (OrderProcessingException e) {
            compensate(reserved); throw e;
        } catch (RuntimeException e) {
            compensate(reserved); throw new OrderProcessingException("Unable to complete order processing", e);
        }
    }

    private ProductResponse loadProduct(Long productId) {
        try {
            ProductResponse response = productClient.findById(productId);
            if (response == null) throw new OrderProcessingException("Product " + productId + " was not found");
            return response;
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Product " + productId + " was not found", e);
        } catch (FeignException e) {
            throw new OrderProcessingException("Unable to obtain product " + productId, e);
        }
    }

    private void reserve(Long productId, Integer quantity) {
        try {
            var response = inventoryClient.reserve(new InventoryRequest(productId, quantity));
            if (response == null || !response.success()) {
                throw new OrderProcessingException("Inventory service rejected reservation for product " + productId);
            }
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Inventory record for product " + productId + " was not found", e);
        } catch (FeignException.Conflict e) {
            throw new OrderProcessingException("Insufficient stock for product " + productId, e);
        } catch (FeignException e) {
            throw new OrderProcessingException("Unable to reserve product " + productId, e);
        }
    }

    private void compensate(Map<Long, Integer> reserved) {
        reserved.forEach((productId, quantity) -> {
            try { inventoryClient.release(new InventoryRequest(productId, quantity)); }
            catch (RuntimeException failure) { log.error("Failed to release reservation for product {}", productId, failure); }
        });
    }

    @Transactional(readOnly = true)
    public OrderDto findById(Long id) {
        return toDto(repository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id)));
    }
    @Transactional(readOnly = true)
    public List<OrderDto> findAll() { return repository.findAll().stream().map(OrderService::toDto).toList(); }
    @Transactional(readOnly = true)
    public List<OrderDto> findByEmail(String email) {
        return repository.findByCustomerEmailOrderByCreatedAtDesc(email).stream().map(OrderService::toDto).toList();
    }
    static OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream().map(i -> new OrderItemDto(i.getId(), i.getProductId(),
                i.getProductName(), i.getQuantity(), i.getPrice(), i.lineTotal())).toList();
        return new OrderDto(order.getId(), order.getCustomerName(), order.getCustomerEmail(), order.getStatus(),
                order.getTotalPrice(), order.getStatusDetails(), order.getCreatedAt(), items);
    }
}
