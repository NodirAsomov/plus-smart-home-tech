package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String customerEmail;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;
    private String statusDetails;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() { }
    public Order(String customerName, String customerEmail) {
        this.customerName = customerName; this.customerEmail = customerEmail;
        this.status = "CREATED"; this.createdAt = LocalDateTime.now(); this.totalPrice = BigDecimal.ZERO;
    }
    public void addItem(OrderItem item) { items.add(item); item.attach(this); totalPrice = totalPrice.add(item.lineTotal()); }
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getStatusDetails() { return statusDetails; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
