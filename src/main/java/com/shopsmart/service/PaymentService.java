
package com.shopsmart.service;

import com.shopsmart.api.ApiDtos.PaymentResponse;
import com.shopsmart.api.ApiMapper;
import com.shopsmart.payment.Payment;
import com.shopsmart.payment.PaymentMethod;
import com.shopsmart.payment.PaymentRepository;
import com.shopsmart.payment.PaymentStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final PaymentRepository payments;

    public PaymentService(PaymentRepository payments) {
        this.payments = payments;
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
}
