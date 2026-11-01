package com.shopsmart.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem,Long> {
    List<CartItem> findByUserEmailOrderByAddedAtAsc(String email);
    Optional<CartItem> findByUserEmailAndProductId(String email,Long productId);
    void deleteByUserEmail(String email);
}
