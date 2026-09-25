package br.com.weg.workshop.registration.service;

import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.dto.RegistrationResponse;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
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

    public RegistrationService(RegistrationRepository registrations, WorkshopRepository workshops, UserRepository users) {
        this.registrations = registrations;
        this.workshops = workshops;
        this.users = users;
    }

    @Transactional
    public RegistrationResponse register(UUID userId, UUID workshopId) {
        UserEntity user = activeUser(userId);
        Workshop workshop = lockedWorkshop(workshopId);
        validateRegistrable(workshop);
        if (registrations.existsByUserIdAndWorkshopIdAndStatusIn(userId, workshopId, VALID_STATUSES)) {
            throw new ConflictException("User already has a valid registration for this workshop.");
        }

        RegistrationStatus status = hasVacancy(workshop) ? registrationStatus(workshop) : RegistrationStatus.WAITING_LIST;
        Registration registration = Registration.create(user, workshop, status, paymentStatus(workshop));
        return RegistrationResponse.from(registrations.save(registration));
    }

    @Transactional
    public RegistrationResponse cancel(UUID userId, UUID registrationId) {
        Registration registration = registrations.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found."));
        if (!registration.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Registration not found.");
        }
        Workshop workshop = lockedWorkshop(registration.getWorkshop().getId());
        if (!VALID_STATUSES.contains(registration.getStatus())) {
            throw new ConflictException("Only valid registrations can be cancelled.");
        }
        boolean releasesVacancy = OCCUPYING_STATUSES.contains(registration.getStatus());
        registration.cancel();
        if (releasesVacancy) {
            promoteFirstEligible(workshop);
        }
        return RegistrationResponse.from(registration);
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
        registrations.findFirstEligibleWaitingListEntry(workshop.getId()).ifPresent(waiting ->
                waiting.promote(registrationStatus(workshop), paymentStatus(workshop)));
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
        UserEntity user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Only active users can register.");
        }
        return user;
    }
}
