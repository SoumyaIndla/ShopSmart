package com.shopsmart.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /*
     * Order history and order details are converted to DTOs in the controller,
     * outside of a transaction. Because spring.jpa.open-in-view=false, the lazy
     * "items", "items.product" and "payment" associations must be fetched up
     * front, otherwise Hibernate throws LazyInitializationException (HTTP 500).
     */
    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    List<CustomerOrder> findByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    Optional<CustomerOrder> findByIdAndUserEmail(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findLockedByIdAndUserEmail(Long id, String email);

    // Used by the admin order list. It is mapped to DTOs outside a transaction.
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "payment"})
    List<CustomerOrder> findAll();
}
