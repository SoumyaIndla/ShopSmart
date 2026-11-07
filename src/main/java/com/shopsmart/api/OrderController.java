
package com.shopsmart.api;

import com.shopsmart.api.ApiDtos.OrderResponse;
import com.shopsmart.payment.PaymentMethod;
import com.shopsmart.service.OrderService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService o) {
        this.orders = o;
    }

    @PostMapping
    public OrderResponse checkout(
            @RequestBody(required = false) ApiDtos.CheckoutRequest request,
            Authentication authentication) {

        PaymentMethod method = request == null
                ? PaymentMethod.COD
                : request.paymentMethod();

        return ApiMapper.order(
                orders.checkout(authentication.getName(), method)
        );
    }

    @GetMapping
    public List<OrderResponse> mine(Authentication authentication) {
        return orders.mine(authentication.getName())
                .stream()
                .map(ApiMapper::order)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(
            @PathVariable Long id,
            Authentication authentication) {

        return ApiMapper.order(
                orders.getMine(authentication.getName(), id)
        );
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return ApiMapper.order(
                orders.cancel(authentication.getName(), id)
        );
    }
}
