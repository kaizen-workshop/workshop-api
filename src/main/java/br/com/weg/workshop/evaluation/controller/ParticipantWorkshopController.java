package br.com.weg.workshop.evaluation.controller;

import br.com.weg.workshop.evaluation.domain.ParticipantWorkshopFilter;
import br.com.weg.workshop.evaluation.service.ParticipantWorkshopService;
import br.com.weg.workshop.shared.pagination.PageResponse;
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
    @GetMapping("/history") @Operation(summary = "List the authenticated user's workshops by lifecycle filter")
    public PageResponse<WorkshopResponse> history(@RequestParam(defaultValue = "COMPLETED") ParticipantWorkshopFilter filter, @PageableDefault(size = 20, sort = "workshop.endDate", direction = Sort.Direction.DESC) Pageable pageable, Authentication authentication) { return PageResponse.from(service.history(userId(authentication), filter, pageable)); }
    @GetMapping("/calendar") @Operation(summary = "List the authenticated user's registered workshops for a date range")
    public PageResponse<WorkshopResponse> calendar(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @PageableDefault(size = 20, sort = "workshop.startDate") Pageable pageable, Authentication authentication) { return PageResponse.from(service.calendar(userId(authentication), from, to, pageable)); }
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
