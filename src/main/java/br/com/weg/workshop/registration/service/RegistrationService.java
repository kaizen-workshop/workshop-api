package br.com.weg.workshop.registration.service;

import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.notification.domain.NotificationType;
import br.com.weg.workshop.notification.service.NotificationService;
import br.com.weg.workshop.registration.dto.RegistrationResponse;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Set<RegistrationStatus> VALID_STATUSES = EnumSet.of(
            RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED, RegistrationStatus.WAITING_LIST);
    private static final Set<RegistrationStatus> OCCUPYING_STATUSES = EnumSet.of(
            RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED);

    private final RegistrationRepository registrations;
    private final WorkshopRepository workshops;
    private final UserRepository users;
    private final NotificationService notifications;
    private final EntityManager entityManager;

    public RegistrationService(RegistrationRepository registrations, WorkshopRepository workshops, UserRepository users,
                               NotificationService notifications, EntityManager entityManager) {
        this.registrations = registrations;
        this.workshops = workshops;
        this.users = users;
        this.notifications = notifications;
        this.entityManager = entityManager;
    }

    @Transactional
    public RegistrationResponse register(UUID userId, UUID workshopId) {
        return register(userId, workshopId, UUID.randomUUID());
    }

    @Transactional
    public RegistrationResponse register(UUID userId, UUID workshopId, UUID idempotencyKey) {
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key is required.");
        UserEntity user = activeUser(userId);
        RegistrationResponse existing = idempotentResult(userId, workshopId, idempotencyKey);
        if (existing != null) return existing;
        Workshop workshop = lockedWorkshop(workshopId);
        existing = idempotentResult(userId, workshopId, idempotencyKey);
        if (existing != null) return existing;
        validateRegistrable(workshop);
        if (registrations.existsByUserIdAndWorkshopIdAndStatusIn(userId, workshopId, VALID_STATUSES)) {
            throw new ConflictException("User already has a valid registration for this workshop.");
        }

        RegistrationStatus status = hasVacancy(workshop) ? registrationStatus(workshop) : RegistrationStatus.WAITING_LIST;
        Registration registration = Registration.create(user, workshop, status, paymentStatus(workshop), idempotencyKey);
        Registration saved = registrations.save(registration);
        NotificationType type = status == RegistrationStatus.WAITING_LIST
                ? NotificationType.WAITING_LIST_JOINED : NotificationType.REGISTRATION_CREATED;
        notifications.notify(userId, type, "Workshop registration", registrationMessage(status),
                Map.of("workshopId", workshopId.toString(), "registrationId", saved.getId().toString()));
        return response(saved);
    }

    private RegistrationResponse idempotentResult(UUID userId, UUID workshopId, UUID idempotencyKey) {
        Registration existing = registrations.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing == null) return null;
        if (!existing.getWorkshop().getId().equals(workshopId)) {
            throw new ConflictException("Idempotency-Key was already used for another workshop.");
        }
        return response(existing);
    }

    @Transactional
    public RegistrationResponse cancel(UUID userId, UUID registrationId) {
        return RegistrationResponse.from(cancelForPayment(userId, registrationId, UUID.randomUUID()).registration());
    }

    @Transactional(readOnly = true)
    public RegistrationResponse currentForWorkshop(UUID userId, UUID workshopId) {
        return registrations.findFirstByUserIdAndWorkshopIdOrderByCreatedAtDesc(userId, workshopId)
                .map(this::response)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));
    }

    @Transactional
    public Registration cancelForPayment(UUID userId, UUID registrationId) {
        return cancelForPayment(userId, registrationId, UUID.randomUUID()).registration();
    }

    @Transactional
    public CancellationResult cancelForPayment(UUID userId, UUID registrationId, UUID idempotencyKey) {
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key is required.");
        Registration registration = registrations.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));
        if (!registration.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Registration not found.");
        }
        Workshop workshop = lockedWorkshop(registration.getWorkshop().getId());
        entityManager.refresh(registration);
        if (idempotencyKey.equals(registration.getCancellationIdempotencyKey())) {
            return new CancellationResult(registration, true);
        }
        if (registrations.existsByUserIdAndCancellationIdempotencyKey(userId, idempotencyKey)) {
            throw new ConflictException("Idempotency-Key was already used for another cancellation.");
        }
        if (!VALID_STATUSES.contains(registration.getStatus())) {
            throw new ConflictException("Only valid registrations can be cancelled.");
        }
        boolean releasesVacancy = OCCUPYING_STATUSES.contains(registration.getStatus());
        registration.recordCancellationKey(idempotencyKey);
        registration.cancel();
        if (releasesVacancy) {
            promoteFirstEligible(workshop);
        }
        return new CancellationResult(registration, false);
    }

    @Transactional
    public Registration cancelAfterPaymentFailure(UUID registrationId) {
        Registration registration = registrations.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));
        Workshop workshop = lockedWorkshop(registration.getWorkshop().getId());
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException("Only pending registrations can be declined.");
        }
        registration.cancel();
        promoteFirstEligible(workshop);
        return registration;
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> listForWorkshop(UUID managerId, boolean admin, UUID workshopId,
                                                       RegistrationStatus status, Pageable pageable) {
        Workshop workshop = workshops.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found."));
        if (!admin && !workshop.getCreatedBy().getId().equals(managerId)) {
            throw new ResourceNotFoundException("Workshop not found.");
        }
        return registrations.findByWorkshop(workshopId, status, pageable).map(RegistrationResponse::from);
    }

    private void promoteFirstEligible(Workshop workshop) {
        registrations.findFirstEligibleWaitingListEntry(workshop.getId()).ifPresent(waiting -> {
            waiting.promote(registrationStatus(workshop), paymentStatus(workshop));
            notifications.notify(waiting.getUser().getId(), NotificationType.WAITING_LIST_PROMOTED,
                    "Waiting list update", "Your registration was promoted from the waiting list.",
                    Map.of("workshopId", workshop.getId().toString(), "registrationId", waiting.getId().toString()));
        });
    }

    private boolean hasVacancy(Workshop workshop) {
        return registrations.countByWorkshopIdAndStatusIn(workshop.getId(), OCCUPYING_STATUSES)
                < workshop.getMaximumParticipants();
    }

    private RegistrationStatus registrationStatus(Workshop workshop) {
        return workshop.getPaymentMethod() == PaymentMethod.FREE ? RegistrationStatus.CONFIRMED : RegistrationStatus.PENDING;
    }

    private RegistrationPaymentStatus paymentStatus(Workshop workshop) {
        return workshop.getPaymentMethod() == PaymentMethod.FREE
                ? RegistrationPaymentStatus.EXEMPT : RegistrationPaymentStatus.PENDING;
    }

    private void validateRegistrable(Workshop workshop) {
        Instant now = Instant.now();
        if (workshop.getStatus() != WorkshopStatus.PUBLISHED) {
            throw new ConflictException("Only published workshops accept registrations.");
        }
        if (now.isBefore(workshop.getRegistrationStart()) || now.isAfter(workshop.getRegistrationEnd())) {
            throw new ConflictException("The registration period is closed.");
        }
    }

    private Workshop lockedWorkshop(UUID workshopId) {
        return workshops.findByIdForUpdate(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found."));
    }

    private UserEntity activeUser(UUID userId) {
        UserEntity user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Only active users can register.");
        }
        return user;
    }

    private String registrationMessage(RegistrationStatus status) {
        return status == RegistrationStatus.WAITING_LIST
                ? "You joined the workshop waiting list."
                : "Your workshop registration was created.";
    }

    private RegistrationResponse response(Registration registration) {
        Long waitingListPosition = registration.getStatus() == RegistrationStatus.WAITING_LIST
                ? registrations.countWaitingAhead(registration.getWorkshop().getId(), registration.getRegisteredAt(),
                        registration.getId()) + 1
                : null;
        return RegistrationResponse.from(registration, waitingListPosition);
    }

    public record CancellationResult(Registration registration, boolean replayed) { }
}
