package br.com.weg.workshop.payment.service;

import br.com.weg.workshop.payment.domain.*;
import br.com.weg.workshop.notification.domain.NotificationType;
import br.com.weg.workshop.notification.service.NotificationService;
import br.com.weg.workshop.payment.dto.PaymentResponse;
import br.com.weg.workshop.payment.gateway.PaymentGateway;
import br.com.weg.workshop.payment.repository.*;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.dto.RegistrationResponse;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.registration.service.RegistrationService;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.workshop.domain.PaymentMethod;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final PaymentEventRepository events;
    private final RegistrationRepository registrations;
    private final RegistrationService registrationService;
    private final PaymentGateway gateway;
    private final ZoneId workshopZone;
    private final NotificationService notifications;

    public PaymentService(PaymentRepository payments, PaymentEventRepository events, RegistrationRepository registrations,
                          RegistrationService registrationService, PaymentGateway gateway,
                          NotificationService notifications,
                          @Value("${app.workshop.time-zone:America/Sao_Paulo}") String workshopTimeZone) {
        this.payments = payments; this.events = events; this.registrations = registrations;
        this.registrationService = registrationService; this.gateway = gateway; this.notifications = notifications;
        this.workshopZone = ZoneId.of(workshopTimeZone);
    }

    @Transactional
    public PaymentResponse create(UUID userId, UUID registrationId, UUID idempotencyKey) {
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key is required.");
        Registration registration = registrations.findByIdForUpdate(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));
        if (!registration.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Registration not found.");
        }
        if (registration.getWorkshop().getPaymentMethod() == PaymentMethod.FREE) {
            throw new ConflictException("Free workshop registrations do not require payment.");
        }
        var byIdempotencyKey = payments.findByIdempotencyKey(idempotencyKey);
        if (byIdempotencyKey.isPresent()) {
            Payment existing = byIdempotencyKey.get();
            if (!existing.getRegistration().getId().equals(registrationId)) {
                throw new ConflictException("Idempotency key was already used for another payment.");
            }
            return PaymentResponse.from(existing);
        }
        var existingPayment = payments.findByRegistrationId(registrationId);
        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            if (!existing.getIdempotencyKey().equals(idempotencyKey)) {
                throw new ConflictException("A payment already exists for this registration.");
            }
            return PaymentResponse.from(existing);
        }
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException("Only pending registrations can start payment.");
        }
        return createPayment(registration, idempotencyKey);
    }

    @Transactional
    public PaymentResponse simulatePaid(UUID actorId, boolean admin, UUID paymentId) {
        requireSimulationAccess(actorId, admin);
        Payment payment = payment(paymentId);
        if (payment.getStatus() == PaymentStatus.PAID) return PaymentResponse.from(payment);
        if (payment.getRegistration().getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException("Only pending registrations can be paid.");
        }
        PaymentStatus previous = payment.getStatus();
        payment.markPaid();
        payment.getRegistration().confirmPayment();
        events.save(PaymentEvent.create(payment, previous));
        notifications.notify(payment.getRegistration().getUser().getId(), NotificationType.PAYMENT_CONFIRMED,
                "Pagamento confirmado", "O pagamento da sua inscrição foi confirmado.",
                java.util.Map.of("paymentId", payment.getId().toString(),
                        "registrationId", payment.getRegistration().getId().toString()));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse simulateDeclined(UUID actorId, boolean admin, UUID paymentId) {
        requireSimulationAccess(actorId, admin);
        Payment payment = payment(paymentId);
        if (payment.getStatus() == PaymentStatus.DECLINED) return PaymentResponse.from(payment);
        PaymentStatus previous = payment.getStatus();
        payment.markDeclined();
        Registration registration = registrationService.cancelAfterPaymentFailure(payment.getRegistration().getId());
        registration.markPaymentDeclined();
        events.save(PaymentEvent.create(payment, previous));
        notifications.notify(payment.getRegistration().getUser().getId(), NotificationType.PAYMENT_DECLINED,
                "Pagamento recusado", "O pagamento da sua inscrição foi recusado e a vaga foi liberada.",
                java.util.Map.of("paymentId", payment.getId().toString(),
                        "registrationId", payment.getRegistration().getId().toString()));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public RegistrationResponse cancelRegistration(UUID userId, UUID registrationId) {
        return cancelRegistration(userId, registrationId, UUID.randomUUID());
    }

    @Transactional
    public RegistrationResponse cancelRegistration(UUID userId, UUID registrationId, UUID idempotencyKey) {
        RegistrationService.CancellationResult result = registrationService.cancelForPayment(userId, registrationId,
                idempotencyKey);
        Registration registration = result.registration();
        if (result.replayed()) return RegistrationResponse.from(registration);
        payments.findByRegistrationId(registrationId).ifPresent(payment -> settleCancellation(payment, registration));
        return RegistrationResponse.from(registration);
    }

    private PaymentResponse createPayment(Registration registration, UUID idempotencyKey) {
        String reference = gateway.createPayment(registration.getWorkshop().getPrice());
        Payment payment = payments.save(Payment.create(registration, registration.getWorkshop().getPrice(),
                registration.getWorkshop().getPaymentMethod(), reference, idempotencyKey));
        events.save(PaymentEvent.create(payment, null));
        return PaymentResponse.from(payment);
    }

    private void settleCancellation(Payment payment, Registration registration) {
        PaymentStatus previous = payment.getStatus();
        if (payment.getStatus() == PaymentStatus.PENDING) {
            gateway.cancelPayment(payment.getExternalReference());
            payment.cancel();
            events.save(PaymentEvent.create(payment, previous));
            return;
        }
        if (payment.getStatus() == PaymentStatus.PAID && eligibleForRefund(registration)) {
            gateway.refundPayment(payment.getExternalReference(), payment.getAmount());
            payment.refund();
            registration.markPaymentRefunded();
            events.save(PaymentEvent.create(payment, previous));
        } else if (payment.getStatus() == PaymentStatus.PAID) {
            registration.markPaymentPaid();
        }
    }

    private boolean eligibleForRefund(Registration registration) {
        if (registration.getWorkshop().isChampionship()) return true;
        Instant refundDeadline = registration.getWorkshop().getStartDate().atTime(registration.getWorkshop().getStartTime())
                .atZone(workshopZone).toInstant().minus(Duration.ofHours(48));
        return Instant.now().isBefore(refundDeadline);
    }

    private Payment payment(UUID paymentId) {
        return payments.findByIdForUpdate(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
    }

    private void requireSimulationAccess(UUID actorId, boolean admin) {
        if (actorId == null || !admin) throw new ResourceNotFoundException("Payment not found.");
    }

}
