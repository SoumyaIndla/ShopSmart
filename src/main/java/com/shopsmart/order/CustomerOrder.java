
package com.shopsmart.order;

import com.shopsmart.user.ShopUser;
import com.shopsmart.payment.Payment;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ShopUser user;

    @OneToOne(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PROCESSING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderLine> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(ShopUser user) {
        this.user = user;
        this.total = BigDecimal.ZERO;
        this.status = OrderStatus.PLACED;
        this.deliveryStatus = DeliveryStatus.PROCESSING;
    }

    // Add order item
    public void add(OrderLine line) {
        items.add(line);
        line.setOrder(this);

        total = total.add(line.getLineTotal());
    }

    // Cancel order
    public void cancel() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only placed orders can be cancelled"
            );
        }

        status = OrderStatus.CANCELLED;
    }

    // Update delivery status
    public void updateDeliveryStatus(DeliveryStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Delivery status cannot be null"
            );
        }

        this.deliveryStatus = newStatus;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public ShopUser getUser() {
        return user;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getItems() {
        return items;
    }

    public Payment getPayment() {
        return payment;
    }

    // Setters
    public void setPayment(Payment payment) {
        this.payment = payment;

        if (payment != null) {
            payment.setOrder(this);
        }
    }
}
