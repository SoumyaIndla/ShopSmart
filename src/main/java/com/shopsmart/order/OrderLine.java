package com.shopsmart.order;

import com.shopsmart.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="order_items")
public class OrderLine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.EAGER) private Product product;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false,precision=10,scale=2) private BigDecimal unitPrice;
    @ManyToOne(fetch=FetchType.LAZY) private CustomerOrder order;
    protected OrderLine(){}
    public OrderLine(Product product,int quantity,BigDecimal unitPrice){this.product=product;this.quantity=quantity;this.unitPrice=unitPrice;}
    void setOrder(CustomerOrder order){this.order=order;}
    public Product getProduct(){return product;} public int getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;}
    public BigDecimal getLineTotal(){return unitPrice.multiply(BigDecimal.valueOf(quantity));}
}
