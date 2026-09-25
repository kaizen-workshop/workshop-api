package br.com.weg.workshop.registration.controller;

import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.dto.RegistrationResponse;
import br.com.weg.workshop.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/workshops/{workshopId}/registrations")
    @Operation(summary = "Register the authenticated user for a workshop")
    public ResponseEntity<RegistrationResponse> register(@PathVariable UUID workshopId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(userId(authentication), workshopId));
    }

    @PatchMapping("/api/v1/registrations/{registrationId}/cancel")
    @Operation(summary = "Cancel the authenticated user's registration")
    public RegistrationResponse cancel(@PathVariable UUID registrationId, Authentication authentication) {
        return service.cancel(userId(authentication), registrationId);
    }

    @GetMapping("/api/v1/workshops/{workshopId}/registrations")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "List registrations for a workshop")
    public Page<RegistrationResponse> list(@PathVariable UUID workshopId,
                                           @RequestParam(required = false) RegistrationStatus status,
                                           @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable,
                                           Authentication authentication) {
        return service.listForWorkshop(userId(authentication), admin(authentication), workshopId, status, pageable);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
    }
}
