
package com.shopsmart.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {
 private ApiDtos() {}

 public record RegisterRequest(
         @NotBlank @Size(max = 80) String fullName,
         @NotBlank @Email @Size(max = 160) String email,
         @NotBlank @Size(min = 8, max = 72) String password
 ) {}

 public record LoginRequest(
         @NotBlank @Email String email,
         @NotBlank String password
 ) {}

 public record AuthResponse(
         String token, String tokenType, String email,
         String fullName, String role
 ) {}

 public record ProductRequest(
         @NotBlank @Size(max = 120) String name,
         @NotBlank @Size(max = 2000) String description,
         @NotNull @DecimalMin("0.01") BigDecimal price,
         @Min(0) int stock,
         @NotBlank @Size(max = 80) String category,
         @Size(max = 1000) String imageUrl
 ) {}

 public record ProductResponse(
         Long id,
         String name,
         String description,
         BigDecimal price,
         int stock,
         String category,
         String imageUrl
 ) {}

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

 public record PageResponse<T>(
         List<T> content, int page, int size,
         long totalElements, int totalPages,
         boolean first, boolean last
 ) {}

 public record AddCartRequest(
         @NotNull Long productId,
         @Min(1) @Max(1000) int quantity
 ) {}

 public record CartQuantityRequest(
         @Min(1) @Max(1000) int quantity
 ) {}

 public record CartLineResponse(
         Long productId, String name,
         BigDecimal unitPrice, int quantity,
         BigDecimal lineTotal
 ) {}

 public record CartResponse(
         List<CartLineResponse> items, BigDecimal total
 ) {}

 public record CheckoutRequest(
         @NotNull com.shopsmart.payment.PaymentMethod paymentMethod
 ) {}

 public record OrderLineResponse(
         Long productId, String name, int quantity,
         BigDecimal unitPrice, BigDecimal lineTotal
 ) {}

 public record OrderResponse(
         Long id,
         String status,
         String deliveryStatus,
         String paymentMethod,
         String paymentStatus,
         Instant createdAt,
         BigDecimal total,
         List<OrderLineResponse> items
 ) {}

 public record ErrorResponse(
         Instant timestamp, int status, String error,
         String message, String path
 ) {}
}
