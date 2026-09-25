package br.com.weg.workshop.payment.repository;

import br.com.weg.workshop.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRegistrationId(UUID registrationId);
    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Payment p where p.id = :id") Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}
