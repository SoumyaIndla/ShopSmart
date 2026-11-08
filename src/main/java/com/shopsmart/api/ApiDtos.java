
package com.shopsmart.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {

 private ApiDtos() {}

 // Registration
 public record RegisterRequest(
         @NotBlank @Size(max = 80) String fullName,
         @NotBlank @Email @Size(max = 160) String email,
         @NotBlank @Size(min = 8, max = 72) String password
 ) {}

 // Login
 public record LoginRequest(
         @NotBlank @Email String email,
         @NotBlank String password
 ) {}

 // Authentication response
 public record AuthResponse(
         String token,
         String tokenType,
         String email,
         String fullName,
         String role
 ) {}

 // Product request
 public record ProductRequest(
         @NotBlank @Size(max = 120) String name,
         @NotBlank @Size(max = 2000) String description,
         @NotNull @DecimalMin("0.01") BigDecimal price,
         @Min(0) int stock,
         @NotBlank @Size(max = 80) String category,
         @Size(max = 1000) String imageUrl
 ) {}

 // Product response
 public record ProductResponse(
         Long id,
         String name,
         String description,
         BigDecimal price,
         int stock,
         String category,
         String imageUrl
 ) {}

 // Admin product response
 public record AdminProductResponse(
         Long id,
         String name,
         String description,
         BigDecimal price,
         int stock,
         String category,
         boolean active,
         String imageUrl
 ) {}

 // Pagination response
 public record PageResponse<T>(
         List<T> content,
         int page,
         int size,
         long totalElements,
         int totalPages,
         boolean first,
         boolean last
 ) {}

 // Add cart request
 public record AddCartRequest(
         @NotNull Long productId,
         @Min(1) @Max(1000) int quantity
 ) {}

 // Update cart quantity
 public record CartQuantityRequest(
         @Min(1) @Max(1000) int quantity
 ) {}

 // Cart line response
 public record CartLineResponse(
         Long productId,
         String name,
         BigDecimal unitPrice,
         int quantity,
         BigDecimal lineTotal
 ) {}

 // Cart response
 public record CartResponse(
         List<CartLineResponse> items,
         BigDecimal total
 ) {}

 // Checkout request
 public record CheckoutRequest(
         @NotNull com.shopsmart.payment.PaymentMethod paymentMethod
 ) {}

 // Order line response
 public record OrderLineResponse(
         Long productId,
         String name,
         int quantity,
         BigDecimal unitPrice,
         BigDecimal lineTotal
 ) {}

 // Order response
 public record OrderResponse(
         Long id,
         String status,
         String deliveryStatus,
         String paymentMethod,
         String paymentStatus,
         Instant createdAt,
         BigDecimal total,
         List<OrderLineResponse> items,
         String customerEmail,
         String customerName
 ) {}

 // Payment response
 public record PaymentResponse(
         Long id,
         Long orderId,
         String method,
         String status,
         BigDecimal amount,
         String razorpayOrderId,
         String razorpayPaymentId,
         Instant createdAt,
         Instant updatedAt
 ) {}

 // Razorpay checkout handshake (key id + order id for checkout.js)
 public record RazorpayOrderResponse(
         String keyId,
         String razorpayOrderId,
         long amount,
         String currency,
         Long orderId,
         String name,
         String email,
         String description
 ) {}

 // Razorpay signature returned by checkout.js
 public record RazorpayVerifyRequest(
         @NotBlank String razorpayOrderId,
         @NotBlank String razorpayPaymentId,
         @NotBlank String razorpaySignature
 ) {}

 // Delivery status update (ADMIN only)
 public record DeliveryStatusUpdateRequest(
         @NotNull com.shopsmart.order.DeliveryStatus deliveryStatus
 ) {}

 // One step in the delivery timeline
 public record TrackingStep(
         String status,
         boolean completed,
         boolean current
 ) {}

 // Delivery tracking response for a customer order
 public record TrackingResponse(
         Long orderId,
         String orderStatus,
         String deliveryStatus,
         List<TrackingStep> timeline
 ) {}

 // Error response
 public record ErrorResponse(
         Instant timestamp,
         int status,
         String error,
         String message,
         String path
 ) {}
}
