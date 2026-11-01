package com.shopsmart.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<ShopUser, Long> {
    Optional<ShopUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
