package com.shopsmart.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface OrderRepository extends JpaRepository<CustomerOrder,Long> {
    List<CustomerOrder> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<CustomerOrder> findByIdAndUserEmail(Long id,String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findLockedByIdAndUserEmail(Long id, String email);
}
