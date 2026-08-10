package ru.yandex.practicum.product.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(length = 500)
    private String description;

    protected Category() { }
    public Category(String name, String description) { this.name = name; this.description = description; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
