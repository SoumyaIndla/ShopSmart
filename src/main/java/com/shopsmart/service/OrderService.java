
package com.shopsmart.service;

import com.shopsmart.cart.CartRepository;
import com.shopsmart.order.*;
import com.shopsmart.product.ProductRepository;
import com.shopsmart.user.UserRepository;
import com.shopsmart.payment.Payment;
import com.shopsmart.payment.PaymentMethod;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;

    public OrderService(
            OrderRepository o,
            CartRepository c,
            ProductRepository p,
            UserRepository u) {

        orders = o;
        carts = c;
        products = p;
        users = u;
    }

    @Transactional
    public CustomerOrder checkout(String email, PaymentMethod paymentMethod) {

        if (paymentMethod != PaymentMethod.COD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Currently only Cash on Delivery is available"
            );
        }

        var cart = carts.findByUserEmailOrderByAddedAtAsc(email);

        if (cart.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your cart is empty"
            );
        }

        var user = users.findByEmail(email).orElseThrow();

        var order = new CustomerOrder(user);

        for (var item : cart) {

            var product = products
                    .findActiveByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "A product is no longer available"
                            )
                    );

            if (product.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for " + product.getName()
                );
            }

            product.reduceStock(item.getQuantity());

            order.add(
                    new OrderLine(
                            product,
                            item.getQuantity(),
                            product.getPrice()
                    )
            );
        }

        // Create COD payment record.
        // Payment remains PENDING until cash is collected.
        Payment payment = new Payment(
                order,
                paymentMethod,
                order.getTotal()
        );

        order.setPayment(payment);

        // Save order and payment together.
        CustomerOrder saved = orders.save(order);

        // Clear cart only after successful order creation.
        carts.deleteAll(cart);

        return saved;
    }

    public List<CustomerOrder> mine(String email) {
        return orders.findByUserEmailOrderByCreatedAtDesc(email);
    }

    public CustomerOrder getMine(String email, Long id) {
        return orders.findByIdAndUserEmail(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );
    }

    @Transactional
    public CustomerOrder cancel(String email, Long id) {

        var order = orders.findLockedByIdAndUserEmail(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order can no longer be cancelled"
            );
        }

        for (var line : order.getItems()) {

            var product = products
                    .findByIdForUpdate(line.getProduct().getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "An order product no longer exists"
                            )
                    );

            product.restoreStock(line.getQuantity());
        }

        order.cancel();

        return order;
    }
}
