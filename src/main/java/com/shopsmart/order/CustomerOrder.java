package com.shopsmart.order;

import com.shopsmart.user.ShopUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="customer_orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private ShopUser user;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrderStatus status=OrderStatus.PLACED;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER)
    private List<OrderLine> items=new ArrayList<>();
    protected CustomerOrder(){}
    public CustomerOrder(ShopUser user){this.user=user;this.total=BigDecimal.ZERO;}
    public void add(OrderLine line){items.add(line);line.setOrder(this);total=total.add(line.getLineTotal());}
    public void cancel(){if(status != OrderStatus.PLACED) throw new IllegalStateException("Only placed orders can be cancelled");status=OrderStatus.CANCELLED;}
    public Long getId(){return id;} public BigDecimal getTotal(){return total;} public OrderStatus getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;} public List<OrderLine> getItems(){return items;}
}
