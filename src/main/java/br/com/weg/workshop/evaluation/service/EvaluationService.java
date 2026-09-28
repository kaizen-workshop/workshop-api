package br.com.weg.workshop.evaluation.service;

import br.com.weg.workshop.evaluation.domain.Evaluation;
import br.com.weg.workshop.evaluation.dto.*;
import br.com.weg.workshop.evaluation.repository.EvaluationRepository;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.Workshop;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {
    private static final Set<RegistrationStatus> ELIGIBLE_STATUSES = EnumSet.of(RegistrationStatus.CONFIRMED);
    private final EvaluationRepository evaluations;
    private final RegistrationRepository registrations;
    private final UserRepository users;
    private final WorkshopRepository workshops;

    public EvaluationService(EvaluationRepository evaluations, RegistrationRepository registrations, UserRepository users, WorkshopRepository workshops) {
        this.evaluations = evaluations; this.registrations = registrations; this.users = users; this.workshops = workshops;
    }

    @Transactional
    public EvaluationResponse create(UUID userId, UUID workshopId, CreateEvaluationRequest request) {
        UserEntity user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Workshop workshop = workshops.findById(workshopId).orElseThrow(() -> new ResourceNotFoundException("Workshop not found."));
        if (workshop.getEndDate().isAfter(LocalDate.now()) || !registrations.existsByUserIdAndWorkshopIdAndStatusIn(userId, workshopId, ELIGIBLE_STATUSES)) {
            throw new ConflictException("The user is not eligible to evaluate this workshop.");
        }
        if (evaluations.existsByUserIdAndWorkshopId(userId, workshopId)) throw new ConflictException("The workshop has already been evaluated.");
        Evaluation evaluation = Evaluation.create(user, workshop, request.rating(), request.comment(), request.contentRating(), request.instructorRating(), request.organizationRating());
        return EvaluationResponse.from(evaluations.save(evaluation));
    }

    @Transactional(readOnly = true)
    public Page<EvaluationResponse> listForWorkshop(UUID managerId, boolean admin, UUID workshopId, Pageable pageable) {
        Workshop workshop = managedWorkshop(managerId, admin, workshopId);
        return evaluations.findByWorkshopId(workshop.getId(), pageable).map(EvaluationResponse::from);
    }

    @Transactional(readOnly = true)
    public EvaluationSummaryResponse summary(UUID managerId, boolean admin, UUID workshopId) {
        Workshop workshop = managedWorkshop(managerId, admin, workshopId);
        Object[] values = evaluations.summarize(workshop.getId());
        return new EvaluationSummaryResponse(workshopId, ((Number) values[4]).longValue(), number(values[0]), number(values[1]), number(values[2]), number(values[3]));
    }

    private Workshop managedWorkshop(UUID managerId, boolean admin, UUID workshopId) {
        Workshop workshop = workshops.findById(workshopId).orElseThrow(() -> new ResourceNotFoundException("Workshop not found."));
        if (!admin && !workshop.getCreatedBy().getId().equals(managerId)) throw new ResourceNotFoundException("Workshop not found.");
        return workshop;
    }
    private Double number(Object value) { return value == null ? null : ((Number) value).doubleValue(); }
}
