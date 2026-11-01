package com.shopsmart.api;
import com.shopsmart.cart.CartItem; import com.shopsmart.order.*; import com.shopsmart.product.Product;
import java.math.BigDecimal; import java.util.List; import static com.shopsmart.api.ApiDtos.*;
public final class ApiMapper {private ApiMapper(){}
 public static ProductResponse product(Product p){return new ProductResponse(p.getId(),p.getName(),p.getDescription(),p.getPrice(),p.getStock(),p.getCategory());}
 public static CartResponse cart(List<CartItem> items){var lines=items.stream().map(i->new CartLineResponse(i.getProduct().getId(),i.getProduct().getName(),i.getProduct().getPrice(),i.getQuantity(),i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();return new CartResponse(lines,lines.stream().map(CartLineResponse::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add));}
 public static OrderResponse order(CustomerOrder o){var lines=o.getItems().stream().map(i->new OrderLineResponse(i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),i.getUnitPrice(),i.getLineTotal())).toList();return new OrderResponse(o.getId(),o.getStatus().name(),o.getCreatedAt(),o.getTotal(),lines);}
}
