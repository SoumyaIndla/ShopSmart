
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
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true
    )
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

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
            CustomerOrder order,
            PaymentMethod method,
            BigDecimal amount
    ) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null"
            );
        }

        if (method == null) {
            throw new IllegalArgumentException(
                    "Payment method cannot be null"
            );
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        this.order = order;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Set order relationship
    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    // Mark payment successful
    public void markSuccess(String razorpayPaymentId) {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be marked successful"
            );
        }

        if (method == PaymentMethod.RAZORPAY &&
                (razorpayPaymentId == null ||
                        razorpayPaymentId.isBlank())) {

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

    // Mark payment failed
    public void markFailed() {

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be marked failed"
            );
        }

        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    // Mark payment refunded
    public void markRefunded() {

        if (status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Only successful payments can be refunded"
            );
        }

        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    // Set Razorpay order ID
    public void setRazorpayOrderId(String razorpayOrderId) {

        if (method != PaymentMethod.RAZORPAY) {
            throw new IllegalStateException(
                    "Razorpay order ID is only allowed for Razorpay payments"
            );
        }

        if (razorpayOrderId == null ||
                razorpayOrderId.isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay order ID cannot be empty"
            );
        }

        this.razorpayOrderId = razorpayOrderId;
        this.updatedAt = Instant.now();
    }

    // Getters
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
}
