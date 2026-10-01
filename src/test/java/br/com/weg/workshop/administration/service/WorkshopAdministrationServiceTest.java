package br.com.weg.workshop.administration.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.administration.dto.*;
import br.com.weg.workshop.audit.service.AuditService;
import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.*;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
class WorkshopAdministrationServiceTest {

    @Mock RegistrationRepository registrations;
    @Mock WorkshopRepository workshops;
    @Mock UserRepository users;
    @Mock AuditService audit;
    @InjectMocks WorkshopAdministrationService service;

    @Test
    void listsParticipantsWithEverySupportedFilter() {
        UserEntity manager = activeUser("Manager");
        Workshop workshop = workshop(manager);
        Registration registration = confirmedRegistration(activeUser("Participant"), workshop);
        Pageable pageable = PageRequest.of(0, 20);
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.findParticipants(workshop.getId(), RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT, AttendanceStatus.ATTENDED, pageable))
                .thenReturn(new PageImpl<>(List.of(registration), pageable, 1));

        var response = service.participants(manager.getId(), false, workshop.getId(), RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT, AttendanceStatus.ATTENDED, pageable);

        assertThat(response.getContent()).singleElement().satisfies(participant -> {
            assertThat(participant.name()).isEqualTo("Participant");
            assertThat(participant.registrationStatus()).isEqualTo("CONFIRMED");
        });
    }

    @Test
    void atomicallyUpdatesAttendanceInRequestOrder() {
        UserEntity manager = activeUser("Manager");
        Workshop workshop = workshop(manager);
        Registration first = confirmedRegistration(activeUser("First"), workshop);
        Registration second = confirmedRegistration(activeUser("Second"), workshop);
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(users.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(registrations.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(second, first));
        BulkAttendanceRequest request = new BulkAttendanceRequest(List.of(
                new AttendanceUpdateRequest(first.getId(), AttendanceStatus.ATTENDED),
                new AttendanceUpdateRequest(second.getId(), AttendanceStatus.JUSTIFIED_ABSENCE)
        ));

        var response = service.updateAttendance(manager.getId(), false, workshop.getId(), request);

        assertThat(response).extracting(ParticipantResponse::registrationId)
                .containsExactly(first.getId(), second.getId());
        assertThat(first.getAttendanceStatus()).isEqualTo(AttendanceStatus.ATTENDED);
        assertThat(second.getAttendanceStatus()).isEqualTo(AttendanceStatus.JUSTIFIED_ABSENCE);
        assertThat(first.getAttendanceMarkedBy()).isSameAs(manager);
    }

    @Test
    void rejectsDuplicateAndIneligibleBulkUpdates() {
        UserEntity manager = activeUser("Manager");
        Workshop workshop = workshop(manager);
        Registration pending = Registration.create(activeUser("Participant"), workshop,
                RegistrationStatus.PENDING, RegistrationPaymentStatus.PENDING);
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(users.findById(manager.getId())).thenReturn(Optional.of(manager));
        BulkAttendanceRequest duplicate = new BulkAttendanceRequest(List.of(
                new AttendanceUpdateRequest(pending.getId(), AttendanceStatus.ABSENT),
                new AttendanceUpdateRequest(pending.getId(), AttendanceStatus.ATTENDED)
        ));

        assertThatThrownBy(() -> service.updateAttendance(manager.getId(), false, workshop.getId(), duplicate))
                .isInstanceOf(ConflictException.class);

        when(registrations.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(pending));
        BulkAttendanceRequest ineligible = new BulkAttendanceRequest(List.of(
                new AttendanceUpdateRequest(pending.getId(), AttendanceStatus.ABSENT)
        ));
        assertThatThrownBy(() -> service.updateAttendance(manager.getId(), false, workshop.getId(), ineligible))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void concealsWorkshopsOwnedByAnotherManager() {
        UserEntity owner = activeUser("Owner");
        UserEntity stranger = activeUser("Stranger");
        Workshop workshop = workshop(owner);
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));

        assertThatThrownBy(() -> service.participants(stranger.getId(), false, workshop.getId(),
                null, null, null, Pageable.unpaged())).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(registrations);
    }

    @Test
    void exportsExcelCompatibleCsvAndXlsx() throws Exception {
        UserEntity manager = activeUser("Manager");
        Workshop workshop = workshop(manager);
        Registration registration = confirmedRegistration(activeUser("Participant, One"), workshop);
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.findParticipants(eq(workshop.getId()), isNull(), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(registration));

        ParticipantExport csv = service.export(manager.getId(), false, workshop.getId(), null, null, null,
                ParticipantExportFormat.CSV);
        ParticipantExport xlsx = service.export(manager.getId(), false, workshop.getId(), null, null, null,
                ParticipantExportFormat.XLSX);

        assertThat(csv.filename()).endsWith(".csv");
        assertThat(new String(csv.content(), java.nio.charset.StandardCharsets.UTF_8))
                .startsWith("\uFEFF\"registrationId\"").contains("\"Participant, One\"");
        assertThat(xlsx.filename()).endsWith(".xlsx");
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx.content()))) {
            List<String> entries = new ArrayList<>();
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) entries.add(entry.getName());
            assertThat(entries).contains("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml");
        }
    }

    @Test
    void buildsManagerDashboardFromScopedCounts() {
        UUID managerId = UUID.randomUUID();
        when(workshops.countManaged(managerId, false, null)).thenReturn(4L);
        when(workshops.countManaged(managerId, false, WorkshopStatus.PUBLISHED)).thenReturn(2L);
        when(registrations.countManaged(managerId, false, null, null)).thenReturn(30L);
        when(registrations.countManaged(managerId, false, RegistrationStatus.CONFIRMED, null)).thenReturn(20L);
        when(registrations.countManaged(managerId, false, RegistrationStatus.WAITING_LIST, null)).thenReturn(5L);
        when(registrations.countManaged(managerId, false, null, AttendanceStatus.ATTENDED)).thenReturn(17L);

        assertThat(service.dashboard(managerId, false)).isEqualTo(new DashboardResponse(4, 2, 30, 20, 5, 17));
    }

    private Registration confirmedRegistration(UserEntity participant, Workshop workshop) {
        return Registration.create(participant, workshop, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT);
    }

    private UserEntity activeUser(String name) {
        UserEntity user = UserEntity.create(name, UUID.randomUUID().toString(), UUID.randomUUID() + "@example.com",
                "WEG-" + UUID.randomUUID(), null, null, "hash", Role.ARWEG);
        user.changePassword("hash");
        return user;
    }

    private Workshop workshop(UserEntity creator) {
        Theme theme = Theme.create("Theme " + UUID.randomUUID(), null);
        Category category = Category.create("Category " + UUID.randomUUID(), null);
        return Workshop.create(new WorkshopData("Workshop", "Description", null, LocalDate.now(), LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON, BigDecimal.ZERO,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600), 30, PaymentMethod.FREE, false, null),
                theme, category, creator);
    }
}
