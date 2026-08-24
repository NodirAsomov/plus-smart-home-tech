package ru.yandex.practicum.order.service;

import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.client.*; import ru.yandex.practicum.order.client.dto.*; import ru.yandex.practicum.order.dto.*; import ru.yandex.practicum.order.entity.*; import ru.yandex.practicum.order.exception.*; import ru.yandex.practicum.order.repository.OrderRepository;
import java.math.BigDecimal; import java.util.*; import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository; private final ProductClient productClient; private final InventoryClient inventoryClient;
    public OrderService(OrderRepository repository, ProductClient productClient, InventoryClient inventoryClient) { this.repository = repository; this.productClient = productClient; this.inventoryClient = inventoryClient; }

    public OrderDto create(CreateOrderRequest request) {
        Map<Long, ProductResponse> products = new HashMap<>(); boolean degraded = false;
        for (Long productId : request.items().stream().map(OrderItemRequest::productId).distinct().toList()) {
            try { ProductResponse product = productClient.findById(productId); if (!Boolean.TRUE.equals(product.active())) throw new OrderProcessingException("Товар снят с продажи: " + productId); products.put(productId, product); }
            catch (ProductServiceUnavailableException e) { degraded = true; log.warn("Creating pending order: product-service unavailable for product {}", productId, e); }
        }
        Map<Long, Integer> quantities = request.items().stream().collect(Collectors.groupingBy(OrderItemRequest::productId, LinkedHashMap::new, Collectors.summingInt(OrderItemRequest::quantity)));
        List<InventoryRequest> reserved = new ArrayList<>();
        try {
            for (var entry : quantities.entrySet()) {
                InventoryRequest reserve = new InventoryRequest(entry.getKey(), entry.getValue());
                try { inventoryClient.reserve(reserve); reserved.add(reserve); }
                catch (InventoryServiceUnavailableException e) { degraded = true; log.warn("Creating pending order: inventory-service unavailable while reserving product {}", entry.getKey(), e); }
            }
            Order order = new Order(request.customerName(), request.customerEmail());
            for (OrderItemRequest item : request.items()) { ProductResponse product = products.get(item.productId()); order.addItem(new OrderItem(item.productId(), product == null ? "Товар #" + item.productId() + " (ожидает проверки)" : product.name(), item.quantity(), product == null ? BigDecimal.ZERO : product.price())); }
            if (degraded) order.markPending("Заказ требует проверки: данные каталога или резерв склада не подтверждены");
            return save(order);
        } catch (RuntimeException e) { compensate(reserved); throw e; }
    }
    private void compensate(List<InventoryRequest> reserved) { for (InventoryRequest reservation : reserved) try { inventoryClient.release(reservation); } catch (RuntimeException e) { log.error("Best-effort compensation failed while releasing product {}", reservation.productId(), e); } }
    @Transactional protected OrderDto save(Order order) { return toDto(repository.save(order)); }
    @Transactional(readOnly = true) public OrderDto findById(Long id) { return toDto(repository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id))); }
    @Transactional(readOnly = true) public List<OrderDto> findAll() { return repository.findAll().stream().map(this::toDto).toList(); }
    @Transactional(readOnly = true) public List<OrderDto> findByEmail(String email) { return repository.findByCustomerEmailOrderByCreatedAtDesc(email).stream().map(this::toDto).toList(); }
    private OrderDto toDto(Order o) { List<OrderItemDto> items = o.getItems().stream().map(i -> new OrderItemDto(i.getId(), i.getProductId(), i.getProductName(), i.getQuantity(), i.getPrice())).toList(); return new OrderDto(o.getId(), o.getCustomerName(), o.getCustomerEmail(), o.getStatus(), o.getTotalPrice(), o.getStatusDetails(), o.getCreatedAt(), items); }
}
