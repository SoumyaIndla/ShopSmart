package com.shopsmart.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Product() {}
    public Product(String name, String description, BigDecimal price, int stock, String category) {
        this.name=name; this.description=description; this.price=price; this.stock=stock; this.category=category;
    }
    public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
    public BigDecimal getPrice(){return price;} public int getStock(){return stock;} public String getCategory(){return category;}
    public boolean isActive(){return active;}
    public void deactivate(){this.active=false;}
    public void update(String name,String description,BigDecimal price,int stock,String category){
        this.name=name;this.description=description;this.price=price;this.stock=stock;this.category=category;this.active=true;
    }
    public void reduceStock(int quantity){ if(quantity<1 || stock<quantity) throw new IllegalArgumentException("Insufficient stock for product: "+name); stock-=quantity; }
    public void restoreStock(int quantity){ if(quantity<1) throw new IllegalArgumentException("Quantity must be positive"); stock+=quantity; }
}
