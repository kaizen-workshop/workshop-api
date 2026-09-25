package br.com.weg.workshop.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_event", schema = "workshop")
public class PaymentEvent {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @Enumerated(EnumType.STRING) private PaymentStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Column(nullable = false) private String externalReference;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected PaymentEvent() { }
    public static PaymentEvent create(Payment payment, PaymentStatus previousStatus) {
        PaymentEvent event = new PaymentEvent(); event.id = UUID.randomUUID(); event.payment = payment;
        event.previousStatus = previousStatus; event.status = payment.getStatus(); event.externalReference = payment.getExternalReference();
        event.createdAt = Instant.now(); return event;
    }
}
