package com.shopsmart.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /*
     * Order history and order details are converted to DTOs in the controller,
     * outside of a transaction. Because spring.jpa.open-in-view=false, the lazy
     * "items", "items.product", "payment" and "user" associations must be fetched
     * up front, otherwise Hibernate throws LazyInitializationException (HTTP 500).
     */
    @EntityGraph(attributePaths = {"items", "items.product", "payment", "user"})
    List<CustomerOrder> findByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"items", "items.product", "payment", "user"})
    Optional<CustomerOrder> findByIdAndUserEmail(Long id, String email);

    /*
     * Native FOR UPDATE avoids Hibernate's "FOR UPDATE OF alias" syntax,
     * which MySQL supports but TiDB does not.
     */
    @Query(value = """
            select o.* from customer_orders o
            join users u on u.id = o.user_id
            where o.id = :id and u.email = :email
            for update
            """, nativeQuery = true)
    Optional<CustomerOrder> findLockedByIdAndUserEmail(
            @Param("id") Long id,
            @Param("email") String email);

    // Used by the admin order list. It is mapped to DTOs outside a transaction.
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "payment", "user"})
    List<CustomerOrder> findAll();
}
