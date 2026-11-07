
package com.shopsmart.admin;

import com.shopsmart.order.OrderRepository;
import com.shopsmart.api.ApiDtos.OrderResponse;
import com.shopsmart.api.ApiDtos.AdminProductResponse;
import com.shopsmart.api.ApiMapper;
import com.shopsmart.product.ProductRepository;
import com.shopsmart.user.UserRepository;
import com.shopsmart.service.PaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

 private final UserRepository users;
 private final OrderRepository orders;
 private final ProductRepository products;
 private final PaymentService paymentService;

 public AdminController(
         UserRepository u,
         OrderRepository o,
         ProductRepository p,
         PaymentService paymentService) {

  users = u;
  orders = o;
  products = p;
  this.paymentService = paymentService;
 }

 public record UserSummary(
         Long id,
         String fullName,
         String email,
         String role
 ) {}

 @GetMapping("/products")
 public com.shopsmart.api.ApiDtos.PageResponse<AdminProductResponse> products(
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {

  if (page < 0 || size < 1 || size > 100) {
   throw new IllegalArgumentException(
           "page must be non-negative and size must be between 1 and 100"
   );
  }

  Page<AdminProductResponse> result = products.findAll(
          PageRequest.of(page, size, Sort.by("name"))
  ).map(p -> new AdminProductResponse(
          p.getId(),
          p.getName(),
          p.getDescription(),
          p.getPrice(),
          p.getStock(),
          p.getCategory(),
          p.isActive(),
          p.getImageUrl()
  ));

  return new com.shopsmart.api.ApiDtos.PageResponse<>(
          result.getContent(),
          result.getNumber(),
          result.getSize(),
          result.getTotalElements(),
          result.getTotalPages(),
          result.isFirst(),
          result.isLast()
  );
 }

 @GetMapping("/products/{id}")
 public AdminProductResponse product(@PathVariable Long id) {

  var p = products.findById(id).orElseThrow(
          () -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Product not found"
          )
  );

  return new AdminProductResponse(
          p.getId(),
          p.getName(),
          p.getDescription(),
          p.getPrice(),
          p.getStock(),
          p.getCategory(),
          p.isActive(),
          p.getImageUrl()
  );
 }

 @GetMapping("/users")
 public List<UserSummary> users() {

  return users.findAll()
          .stream()
          .map(u -> new UserSummary(
                  u.getId(),
                  u.getFullName(),
                  u.getEmail(),
                  u.getRole().name()
          ))
          .toList();
 }

 @GetMapping("/orders")
 public List<OrderResponse> orders() {

  return orders.findAll()
          .stream()
          .map(ApiMapper::order)
          .toList();
 }

 // Confirm COD payment after collecting cash from the customer.
 @PostMapping("/orders/{orderId}/confirm-cod-payment")
 public String confirmCodPayment(@PathVariable Long orderId) {

  paymentService.confirmCodPayment(orderId);

  return "COD payment confirmed successfully";
 }
}
