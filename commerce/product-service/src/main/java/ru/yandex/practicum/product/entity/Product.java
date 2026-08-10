package ru.yandex.practicum.product.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    private String imageUrl;
    @Column(nullable = false)
    private boolean active = true;

    protected Product() { }
    public Product(String name, String description, BigDecimal price, Category category, String imageUrl) {
        this.name = name; this.description = description; this.price = price; this.category = category; this.imageUrl = imageUrl;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Category getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
    public void update(String name, String description, BigDecimal price, Category category, String imageUrl, Boolean active) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (category != null) this.category = category;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (active != null) this.active = active;
    }
}
