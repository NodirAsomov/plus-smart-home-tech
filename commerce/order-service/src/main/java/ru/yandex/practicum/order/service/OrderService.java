package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.client.*;
import ru.yandex.practicum.order.client.dto.*;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.*;
import ru.yandex.practicum.order.exception.*;
import ru.yandex.practicum.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository; private final ProductClient productClient;
    private final InventoryClient inventoryClient; private final OrderPersistenceService persistenceService;
    public OrderService(OrderRepository repository, ProductClient productClient, InventoryClient inventoryClient,
                        OrderPersistenceService persistenceService) {
        this.repository = repository; this.productClient = productClient;
        this.inventoryClient = inventoryClient; this.persistenceService = persistenceService;
    }

    public OrderDto create(CreateOrderRequest request) {
        Map<Long,Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(i -> quantities.merge(i.productId(), i.quantity(), Integer::sum));
        Map<Long,ProductResponse> products = new LinkedHashMap<>();
        List<String> degradationReasons = new ArrayList<>();
        quantities.keySet().forEach(id -> {
            try {
                ProductResponse p = product(id);
                if (!Boolean.TRUE.equals(p.active())) throw new OrderProcessingException("Product " + id + " is not available for sale");
                products.put(id, p);
            } catch (ProductServiceUnavailableException e) {
                log.warn("Order will require manual confirmation: {}", e.getMessage());
                products.put(id, new ProductResponse(id, "Товар #" + id + " (ожидает проверки)", BigDecimal.ZERO, true));
                degradationReasons.add("Не удалось проверить данные товара " + id);
            }
        });
        Map<Long,Integer> reserved = new LinkedHashMap<>();
        try {
            quantities.forEach((id, qty) -> {
                try {
                    reserve(id, qty);
                    reserved.put(id, qty);
                } catch (InventoryServiceUnavailableException e) {
                    log.warn("Order will require manual confirmation: {}", e.getMessage());
                    degradationReasons.add("Не удалось подтвердить резерв товара " + id);
                }
            });
            Order order = new Order(request.customerName(), request.customerEmail());
            request.items().forEach(i -> {
                ProductResponse p = products.get(i.productId());
                order.addItem(new OrderItem(p.id(), p.name(), i.quantity(), p.price()));
            });
            if (!degradationReasons.isEmpty()) {
                order.markPendingConfirmation("Заказ требует ручной проверки: " + String.join("; ", degradationReasons));
            }
            return persistenceService.save(order);
        } catch (OrderProcessingException e) { compensate(reserved); throw e; }
        catch (RuntimeException e) { compensate(reserved); throw new OrderProcessingException("Unable to complete order processing", e); }
    }
    private ProductResponse product(Long id) {
        try {
            ProductResponse p = productClient.findById(id);
            if (p == null) throw new OrderProcessingException("Product " + id + " was not found");
            return p;
        } catch (FeignException.NotFound e) { throw new OrderProcessingException("Product " + id + " was not found", e); }
        catch (ProductServiceUnavailableException e) { throw e; }
        catch (FeignException e) { throw new ProductServiceUnavailableException(id, e); }
    }
    private void reserve(Long id, Integer qty) {
        try {
            InventoryResponse response = inventoryClient.reserve(new InventoryRequest(id, qty));
            if (response == null || !response.success()) throw new OrderProcessingException("Inventory service rejected reservation for product " + id);
        } catch (FeignException.NotFound e) { throw new OrderProcessingException("Inventory record for product " + id + " was not found", e); }
        catch (FeignException.Conflict e) { throw new OrderProcessingException("Insufficient stock for product " + id, e); }
        catch (InventoryServiceUnavailableException e) { throw e; }
        catch (FeignException e) { throw new InventoryServiceUnavailableException("reserving", id, e); }
    }
    private void compensate(Map<Long,Integer> reserved) {
        reserved.forEach((id, qty) -> { try { inventoryClient.release(new InventoryRequest(id, qty)); }
            catch (RuntimeException e) { log.error("Failed to release reservation for product {}", id, e); } });
    }
    @Transactional(readOnly = true) public OrderDto findById(Long id) { return toDto(repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found: " + id))); }
    @Transactional(readOnly = true) public List<OrderDto> findAll() { return repository.findAll().stream().map(OrderService::toDto).toList(); }
    @Transactional(readOnly = true) public List<OrderDto> findByEmail(String email) { return repository
            .findByCustomerEmailOrderByCreatedAtDesc(email).stream().map(OrderService::toDto).toList(); }
    static OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(i -> new OrderItemDto(i.getId(), i.getProductId(),
                i.getProductName(), i.getQuantity(), i.getPrice(), i.lineTotal())).toList();
        return new OrderDto(o.getId(), o.getCustomerName(), o.getCustomerEmail(), o.getStatus(), o.getTotalPrice(),
                o.getStatusDetails(), o.getCreatedAt(), items);
    }
}
