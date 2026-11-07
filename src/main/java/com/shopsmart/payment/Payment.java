
package com.shopsmart.payment;

import com.shopsmart.order.CustomerOrder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private Instant createdAt;

    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(CustomerOrder order, PaymentMethod method,
                   BigDecimal amount) {
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }


    public void markSuccess(String razorpayPaymentId) {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be marked successful"
            );
        }

        if (method == PaymentMethod.RAZORPAY &&
                (razorpayPaymentId == null || razorpayPaymentId.isBlank())) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is required"
            );
        }

        this.status = PaymentStatus.SUCCESS;

        if (method == PaymentMethod.RAZORPAY) {
            this.razorpayPaymentId = razorpayPaymentId;
        }

        this.updatedAt = Instant.now();
    }


    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
        this.updatedAt = Instant.now();
    }
}

