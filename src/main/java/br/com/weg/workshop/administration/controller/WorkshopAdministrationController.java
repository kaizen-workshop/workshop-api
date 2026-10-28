package br.com.weg.workshop.administration.controller;

import br.com.weg.workshop.administration.dto.*;
import br.com.weg.workshop.administration.service.WorkshopAdministrationService;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/arweg")
public class WorkshopAdministrationController {

    private final WorkshopAdministrationService service;

    public WorkshopAdministrationController(WorkshopAdministrationService service) {
        this.service = service;
    }

    @GetMapping("/workshops/{workshopId}/participants")
    @Operation(summary = "List and filter participants of a managed workshop")
    public PageResponse<ParticipantResponse> participants(
            @PathVariable UUID workshopId,
            @RequestParam(required = false) RegistrationStatus registrationStatus,
            @RequestParam(required = false) RegistrationPaymentStatus paymentStatus,
            @RequestParam(required = false) AttendanceStatus attendanceStatus,
            @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable,
            Authentication authentication) {
        return PageResponse.from(service.participants(userId(authentication), admin(authentication), workshopId,
                registrationStatus, paymentStatus, attendanceStatus, pageable));
    }

    @PatchMapping("/workshops/{workshopId}/attendance")
    @Operation(summary = "Bulk update attendance for a managed workshop")
    public List<ParticipantResponse> updateAttendance(@PathVariable UUID workshopId,
                                                       @Valid @RequestBody BulkAttendanceRequest request,
                                                       Authentication authentication) {
        return service.updateAttendance(userId(authentication), admin(authentication), workshopId, request);
    }

    @GetMapping("/workshops/{workshopId}/participants/export")
    @Operation(summary = "Export filtered workshop participants as CSV or XLSX")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID workshopId,
            @RequestParam(required = false) RegistrationStatus registrationStatus,
            @RequestParam(required = false) RegistrationPaymentStatus paymentStatus,
            @RequestParam(required = false) AttendanceStatus attendanceStatus,
            @RequestParam(defaultValue = "CSV") ParticipantExportFormat format,
            Authentication authentication) {
        ParticipantExport export = service.export(userId(authentication), admin(authentication), workshopId,
                registrationStatus, paymentStatus, attendanceStatus, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .contentLength(export.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename(), StandardCharsets.UTF_8).build().toString())
                .body(export.content());
    }

    @GetMapping("/workshops/{workshopId}/payments")
    @Operation(summary = "List the payments of a managed workshop")
    public List<WorkshopPaymentResponse> payments(@PathVariable UUID workshopId, Authentication authentication) {
        return service.payments(userId(authentication), admin(authentication), workshopId);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Return the ARWEG workshop administration dashboard")
    public DashboardResponse dashboard(Authentication authentication) {
        return service.dashboard(userId(authentication), admin(authentication));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
