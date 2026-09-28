package br.com.weg.workshop.evaluation.controller;

import br.com.weg.workshop.evaluation.service.ParticipantWorkshopService;
import br.com.weg.workshop.workshop.dto.WorkshopResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/workshops")
public class ParticipantWorkshopController {
    private final ParticipantWorkshopService service;
    public ParticipantWorkshopController(ParticipantWorkshopService service) { this.service = service; }
    @GetMapping("/history") @Operation(summary = "List completed workshops attended by the authenticated user")
    public Page<WorkshopResponse> history(@PageableDefault(size = 20, sort = "workshop.endDate", direction = Sort.Direction.DESC) Pageable pageable, Authentication authentication) { return service.history(userId(authentication), pageable); }
    @GetMapping("/calendar") @Operation(summary = "List the authenticated user's registered workshops for a date range")
    public Page<WorkshopResponse> calendar(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @PageableDefault(size = 20, sort = "workshop.startDate") Pageable pageable, Authentication authentication) { return service.calendar(userId(authentication), from, to, pageable); }
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
