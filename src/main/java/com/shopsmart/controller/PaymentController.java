
package com.shopsmart.controller;

import com.shopsmart.api.ApiDtos.PaymentResponse;
import com.shopsmart.service.PaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Customer can view their own payment
    @GetMapping("/order/{orderId}")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentResponse getPaymentByOrderId(
            @PathVariable Long orderId,
            Authentication authentication) {

        return paymentService.getPaymentByOrderId(
                orderId,
                authentication.getName()
        );
    }

    // Only ADMIN can confirm COD payment
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cod/{orderId}/confirm")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentResponse confirmCodPayment(
            @PathVariable Long orderId) {

        return paymentService.confirmCodPayment(orderId);
    }
}
