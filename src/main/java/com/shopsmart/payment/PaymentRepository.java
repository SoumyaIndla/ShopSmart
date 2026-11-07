
package com.shopsmart.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment using order ID
    Optional<Payment> findByOrderId(Long orderId);

    // Find payment only when the order belongs to this customer
    Optional<Payment> findByOrder_IdAndOrder_User_Email(
            Long orderId,
            String email
    );
}
