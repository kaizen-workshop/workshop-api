package br.com.weg.workshop.evaluation.service;

import br.com.weg.workshop.evaluation.domain.ParticipantWorkshopFilter;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.workshop.dto.WorkshopResponse;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantWorkshopService {
    private final RegistrationRepository registrations;
    public ParticipantWorkshopService(RegistrationRepository registrations) { this.registrations = registrations; }
    @Transactional(readOnly = true)
    public Page<WorkshopResponse> history(UUID userId, Pageable pageable) {
        return history(userId, ParticipantWorkshopFilter.COMPLETED, pageable);
    }
    @Transactional(readOnly = true)
    public Page<WorkshopResponse> history(UUID userId, ParticipantWorkshopFilter filter, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<br.com.weg.workshop.registration.domain.Registration> result = switch (filter) {
            case FUTURE -> registrations.findFutureByUser(userId,
                    EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED), today, pageable);
            case IN_PROGRESS -> registrations.findInProgressByUser(userId, today, pageable);
            case COMPLETED -> registrations.findCompletedByUser(userId,
                    EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.REFUNDED), today, pageable);
            case CANCELLED -> registrations.findCancelledByUser(userId, pageable);
            case WAITING_LIST -> registrations.findByUserIdAndStatus(userId, RegistrationStatus.WAITING_LIST, pageable);
        };
        return result
                .map(registration -> WorkshopResponse.from(registration.getWorkshop()));
    }
    @Transactional(readOnly = true)
    public Page<WorkshopResponse> calendar(UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException("Calendar end date must not precede the start date.");
        return registrations.findCalendarByUser(userId, EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED), from, to, pageable)
                .map(registration -> WorkshopResponse.from(registration.getWorkshop()));
    }
}
