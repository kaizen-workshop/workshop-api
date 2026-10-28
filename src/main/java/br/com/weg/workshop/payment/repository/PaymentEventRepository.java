package br.com.weg.workshop.payment.repository;

import br.com.weg.workshop.payment.domain.PaymentEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> { }
