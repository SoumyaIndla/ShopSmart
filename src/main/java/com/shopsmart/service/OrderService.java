
package com.shopsmart.service;

import com.shopsmart.cart.CartRepository;
import com.shopsmart.order.*;
import com.shopsmart.product.ProductRepository;
import com.shopsmart.user.UserRepository;
import com.shopsmart.payment.Payment;
import com.shopsmart.payment.PaymentMethod;
import com.shopsmart.api.ApiDtos.OrderResponse;
import com.shopsmart.api.ApiDtos.TrackingResponse;
import com.shopsmart.api.ApiDtos.TrackingStep;
import com.shopsmart.api.ApiMapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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

        if (paymentMethod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required"
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

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.RETURNED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order can no longer be cancelled"
            );
        }

        // Once a parcel has left the warehouse it cannot be cancelled.
        if (order.getDeliveryStatus() == DeliveryStatus.SHIPPED
                || order.getDeliveryStatus() == DeliveryStatus.OUT_FOR_DELIVERY
                || order.getDeliveryStatus() == DeliveryStatus.DELIVERED
                || order.getDeliveryStatus() == DeliveryStatus.RETURNED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been shipped and cannot be cancelled"
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

    // Delivery lifecycle shown to customers, in order.
    private static final List<DeliveryStatus> DELIVERY_FLOW = List.of(
            DeliveryStatus.PROCESSING,
            DeliveryStatus.CONFIRMED,
            DeliveryStatus.PACKED,
            DeliveryStatus.SHIPPED,
            DeliveryStatus.OUT_FOR_DELIVERY,
            DeliveryStatus.DELIVERED
    );

    // ADMIN: move an order through the delivery lifecycle.
    @Transactional
    public OrderResponse updateDeliveryStatus(Long orderId, DeliveryStatus status) {

        var order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        order.updateDeliveryStatus(status);

        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.syncOrderStatus(orderStatusFor(status));
        }

        // Mapped inside the transaction so lazy items/payment can be read.
        return ApiMapper.order(order);
    }

    private static OrderStatus orderStatusFor(DeliveryStatus status) {
        return switch (status) {
            case PROCESSING -> OrderStatus.PLACED;
            case CONFIRMED -> OrderStatus.CONFIRMED;
            case PACKED -> OrderStatus.PACKED;
            case SHIPPED -> OrderStatus.SHIPPED;
            case OUT_FOR_DELIVERY -> OrderStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> OrderStatus.DELIVERED;
            case RETURNED -> OrderStatus.RETURNED;
        };
    }

    // CUSTOMER: current delivery status plus a timeline derived from the enum.
    @Transactional(readOnly = true)
    public TrackingResponse track(String email, Long id) {

        var order = getMine(email, id);
        var current = order.getDeliveryStatus();

        var timeline = new ArrayList<TrackingStep>();

        for (var step : DELIVERY_FLOW) {

            boolean completed = current != DeliveryStatus.RETURNED
                    && step.ordinal() <= current.ordinal();

            timeline.add(new TrackingStep(
                    step.name(),
                    completed,
                    step == current
            ));
        }

        if (current == DeliveryStatus.RETURNED) {
            timeline.add(new TrackingStep(current.name(), true, true));
        }

        return new TrackingResponse(
                order.getId(),
                order.getStatus().name(),
                current.name(),
                timeline
        );
    }
}
