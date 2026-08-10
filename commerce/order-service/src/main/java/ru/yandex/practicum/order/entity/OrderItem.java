package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false)
    private String productName;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    protected OrderItem() { }
    public OrderItem(Long productId, String productName, int quantity, BigDecimal price) {
        this.productId = productId; this.productName = productName; this.quantity = quantity; this.price = price;
    }
    void attach(Order order) { this.order = order; }
    BigDecimal lineTotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
}
