package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false)
    private int reservedQuantity;
    @Version
    private Long version;

    protected Inventory() { }
    public Inventory(Long productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getAvailableQuantity() { return quantity - reservedQuantity; }
    public void setQuantity(int quantity) {
        if (quantity < reservedQuantity) throw new IllegalArgumentException("Quantity cannot be less than reserved quantity");
        this.quantity = quantity;
    }
    public void reserve(int requested) { this.reservedQuantity += requested; }
    public void release(int requested) {
        if (requested > reservedQuantity) throw new IllegalArgumentException(
                "Cannot release " + requested + " units; only " + reservedQuantity + " reserved for product " + productId);
        this.reservedQuantity -= requested;
    }
}
