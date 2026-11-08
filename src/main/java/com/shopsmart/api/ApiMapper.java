
package com.shopsmart.api;

import com.shopsmart.cart.CartItem;
import com.shopsmart.order.*;
import com.shopsmart.product.Product;
import com.shopsmart.payment.Payment;

import java.math.BigDecimal;
import java.util.List;

import static com.shopsmart.api.ApiDtos.*;

public final class ApiMapper {

 private ApiMapper() {}

 // Convert Product entity to ProductResponse
 public static ProductResponse product(Product p) {

  return new ProductResponse(
          p.getId(),
          p.getName(),
          p.getDescription(),
          p.getPrice(),
          p.getStock(),
          p.getCategory(),
          p.getImageUrl()
  );
 }

 // Convert cart items to CartResponse
 public static CartResponse cart(List<CartItem> items) {

  var lines = items.stream()
          .map(i -> new CartLineResponse(
                  i.getProduct().getId(),
                  i.getProduct().getName(),
                  i.getProduct().getPrice(),
                  i.getQuantity(),
                  i.getProduct().getPrice()
                          .multiply(BigDecimal.valueOf(i.getQuantity()))
          ))
          .toList();

  return new CartResponse(
          lines,
          lines.stream()
                  .map(CartLineResponse::lineTotal)
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
  );
 }

 // Convert CustomerOrder entity to OrderResponse
 public static OrderResponse order(CustomerOrder o) {

  var lines = o.getItems().stream()
          .map(i -> new OrderLineResponse(
                  i.getProduct().getId(),
                  i.getProduct().getName(),
                  i.getQuantity(),
                  i.getUnitPrice(),
                  i.getLineTotal()
          ))
          .toList();

  var payment = o.getPayment();

  return new OrderResponse(
          o.getId(),
          o.getStatus().name(),
          o.getDeliveryStatus().name(),
          payment != null ? payment.getMethod().name() : null,
          payment != null ? payment.getStatus().name() : null,
          o.getCreatedAt(),
          o.getTotal(),
          lines,
          o.getUser().getEmail(),
          o.getUser().getFullName()
  );
 }

 // Convert Payment entity to PaymentResponse
 public static PaymentResponse payment(Payment p) {

  return new PaymentResponse(
          p.getId(),
          p.getOrder().getId(),
          p.getMethod().name(),
          p.getStatus().name(),
          p.getAmount(),
          p.getRazorpayOrderId(),
          p.getRazorpayPaymentId(),
          p.getCreatedAt(),
          p.getUpdatedAt()
  );
 }
}
