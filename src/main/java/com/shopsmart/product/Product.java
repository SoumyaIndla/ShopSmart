
package com.shopsmart.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 80)
    private String category;

    // New product image field
    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Product() {}

    // Existing constructor - preserved
    public Product(String name, String description,
                   BigDecimal price, int stock, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    // New constructor with image URL
    public Product(String name, String description,
                   BigDecimal price, int stock,
                   String category, String imageUrl) {
        this(name, description, price, stock, category);
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        this.active = false;
    }

    // Existing update method - preserved
    public void update(String name, String description,
                       BigDecimal price, int stock, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.active = true;
    }

    // New update method with image URL
    public void update(String name, String description,
                       BigDecimal price, int stock,
                       String category, String imageUrl) {
        update(name, description, price, stock, category);
        this.imageUrl = imageUrl;
    }

    public void reduceStock(int quantity) {
        if (quantity < 1 || stock < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + name
            );
        }
        stock -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be positive"
            );
        }
        stock += quantity;
    }
}
