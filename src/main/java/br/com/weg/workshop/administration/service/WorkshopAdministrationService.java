package br.com.weg.workshop.administration.service;
import br.com.weg.workshop.audit.service.AuditService;

import br.com.weg.workshop.administration.dto.*;
import br.com.weg.workshop.payment.repository.PaymentRepository;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkshopAdministrationService {

    private static final Set<RegistrationStatus> ATTENDANCE_ELIGIBLE =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.REFUNDED);

    private final RegistrationRepository registrations;
    private final WorkshopRepository workshops;
    private final UserRepository users;
    private final AuditService audit;
    private final PaymentRepository payments;

    public WorkshopAdministrationService(RegistrationRepository registrations, WorkshopRepository workshops,
                                         UserRepository users, AuditService audit, PaymentRepository payments) {
        this.payments = payments;
        this.registrations = registrations;
        this.workshops = workshops;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<ParticipantResponse> participants(UUID managerId, boolean admin, UUID workshopId,
                                                   RegistrationStatus registrationStatus,
                                                   RegistrationPaymentStatus paymentStatus,
                                                   AttendanceStatus attendanceStatus, Pageable pageable) {
        managedWorkshop(managerId, admin, workshopId);
        return registrations.findParticipants(workshopId, registrationStatus, paymentStatus, attendanceStatus, pageable)
                .map(ParticipantResponse::from);
    }

    @Transactional(readOnly = true)
    public List<WorkshopPaymentResponse> payments(UUID managerId, boolean admin, UUID workshopId) {
        managedWorkshop(managerId, admin, workshopId);
        return payments.findByWorkshopId(workshopId).stream().map(WorkshopPaymentResponse::from).toList();
    }

    @Transactional
    public List<ParticipantResponse> updateAttendance(UUID managerId, boolean admin, UUID workshopId,
                                                       BulkAttendanceRequest request) {
        managedWorkshop(managerId, admin, workshopId);
        UserEntity actor = users.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Set<UUID> ids = request.updates().stream().map(AttendanceUpdateRequest::registrationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.size() != request.updates().size()) {
            throw new ConflictException("A registration can only appear once in a bulk attendance update.");
        }
        Map<UUID, Registration> found = registrations.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Registration::getId, Function.identity()));
        if (found.size() != ids.size()) throw new ResourceNotFoundException("Registration not found.");

        List<ParticipantResponse> responses = new ArrayList<>();
        for (AttendanceUpdateRequest update : request.updates()) {
            Registration registration = found.get(update.registrationId());
            if (!registration.getWorkshop().getId().equals(workshopId)) {
                throw new ResourceNotFoundException("Registration not found.");
            }
            if (!ATTENDANCE_ELIGIBLE.contains(registration.getStatus())) {
                throw new ConflictException("Attendance requires a confirmed or refunded registration.");
            }
            String previous = registration.getAttendanceStatus() == null ? null : registration.getAttendanceStatus().name();
            registration.markAttendance(update.status(), actor);
            audit.record(managerId, "MARK_ATTENDANCE", "REGISTRATION", registration.getId(),
                    previous, update.status().name());
            responses.add(ParticipantResponse.from(registration));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public ParticipantExport export(UUID managerId, boolean admin, UUID workshopId,
                                    RegistrationStatus registrationStatus,
                                    RegistrationPaymentStatus paymentStatus,
                                    AttendanceStatus attendanceStatus, ParticipantExportFormat format) {
        Workshop workshop = managedWorkshop(managerId, admin, workshopId);
        List<ParticipantResponse> participants = registrations.findParticipants(workshopId, registrationStatus,
                        paymentStatus, attendanceStatus, Sort.by("registeredAt").ascending().and(Sort.by("id")))
                .stream().map(ParticipantResponse::from).toList();
        String baseName = "workshop-" + workshop.getId() + "-participants";
        return switch (format) {
            case CSV -> new ParticipantExport(ParticipantExportWriter.csv(participants), "text/csv;charset=UTF-8",
                    baseName + ".csv");
            case XLSX -> new ParticipantExport(ParticipantExportWriter.xlsx(participants),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", baseName + ".xlsx");
        };
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID managerId, boolean admin) {
        return new DashboardResponse(
                workshops.countManaged(managerId, admin, null),
                workshops.countManaged(managerId, admin, WorkshopStatus.PUBLISHED),
                registrations.countManaged(managerId, admin, null, null),
                registrations.countManaged(managerId, admin, RegistrationStatus.CONFIRMED, null),
                registrations.countManaged(managerId, admin, RegistrationStatus.WAITING_LIST, null),
                registrations.countManaged(managerId, admin, null, AttendanceStatus.ATTENDED)
        );
    }

    private Workshop managedWorkshop(UUID managerId, boolean admin, UUID workshopId) {
        Workshop workshop = workshops.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found."));
        if (!admin && !workshop.getCreatedBy().getId().equals(managerId)) {
            throw new ResourceNotFoundException("Workshop not found.");
        }
        return workshop;
    }
}
