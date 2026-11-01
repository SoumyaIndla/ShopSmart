package com.shopsmart.cart;

import com.shopsmart.product.Product;
import com.shopsmart.user.ShopUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="cart_items", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","product_id"}))
public class CartItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) private ShopUser user;
    @ManyToOne(optional=false,fetch=FetchType.EAGER) private Product product;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false) private Instant addedAt=Instant.now();
    protected CartItem(){}
    public CartItem(ShopUser user,Product product,int quantity){this.user=user;this.product=product;this.quantity=quantity;}
    public Long getId(){return id;} public Product getProduct(){return product;} public int getQuantity(){return quantity;}
    public void setQuantity(int quantity){this.quantity=quantity;}
}
