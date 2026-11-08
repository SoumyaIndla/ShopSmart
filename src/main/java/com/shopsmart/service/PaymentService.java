
package com.shopsmart.service;

import com.shopsmart.api.ApiDtos.PaymentResponse;
import com.shopsmart.api.ApiDtos.RazorpayOrderResponse;
import com.shopsmart.api.ApiDtos.RazorpayVerifyRequest;
import com.shopsmart.api.ApiMapper;
import com.shopsmart.payment.Payment;
import com.shopsmart.payment.PaymentMethod;
import com.shopsmart.payment.PaymentRepository;
import com.shopsmart.payment.PaymentStatus;
import com.shopsmart.payment.RazorpayService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final RazorpayService razorpay;

    public PaymentService(
            PaymentRepository payments,
            RazorpayService razorpay) {
        this.payments = payments;
        this.razorpay = razorpay;
    }

    // Customer can view only their own order payment
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, String email) {

        Payment payment = payments
                .findByOrder_IdAndOrder_User_Email(orderId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        return ApiMapper.payment(payment);
    }

    // Only admin should confirm COD payment
    @Transactional
    public PaymentResponse confirmCodPayment(Long orderId) {

        Payment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        if (payment.getMethod() != PaymentMethod.COD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This is not a COD payment"
            );
        }

        // A cancelled order must never be marked as paid.
        if (payment.getOrder().getStatus() == com.shopsmart.order.OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot confirm payment for a cancelled order"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not pending"
            );
        }

        payment.markSuccess(null);

        return ApiMapper.payment(payment);
    }

    // CUSTOMER: create (or reuse) a Razorpay order for checkout.js
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long orderId, String email) {

        Payment payment = payments
                .findByOrder_IdAndOrder_User_Email(orderId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getMethod() != PaymentMethod.RAZORPAY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order is not a Razorpay payment"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not pending"
            );
        }

        if (payment.getOrder().getStatus()
                == com.shopsmart.order.OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot pay for a cancelled order"
            );
        }

        String razorpayOrderId = payment.getRazorpayOrderId();

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            razorpayOrderId = razorpay.createOrder(
                    payment.getAmount(),
                    orderId
            );

            payment.setRazorpayOrderId(razorpayOrderId);
        }

        long amountInPaise = payment.getAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValueExact();

        return new RazorpayOrderResponse(
                razorpay.getKeyId(),
                razorpayOrderId,
                amountInPaise,
                "INR",
                orderId,
                "ShopSmart",
                email,
                "Order #" + orderId
        );
    }

    // CUSTOMER: verify the checkout.js signature, then mark the payment paid
    @Transactional
    public PaymentResponse verifyRazorpayPayment(
            RazorpayVerifyRequest request,
            String email) {

        Payment payment = payments
                .findByRazorpayOrderIdAndOrder_User_Email(
                        request.razorpayOrderId(),
                        email
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not pending"
            );
        }

        boolean valid = razorpay.verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay signature"
            );
        }

        payment.markSuccess(request.razorpayPaymentId());

        return ApiMapper.payment(payment);
    }
}
