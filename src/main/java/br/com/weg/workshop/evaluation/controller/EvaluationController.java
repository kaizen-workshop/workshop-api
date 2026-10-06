package br.com.weg.workshop.evaluation.controller;

import br.com.weg.workshop.evaluation.dto.*;
import br.com.weg.workshop.evaluation.service.EvaluationService;
import br.com.weg.workshop.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class EvaluationController {
    private final EvaluationService service;
    public EvaluationController(EvaluationService service) { this.service = service; }
    @PostMapping("/api/v1/workshops/{workshopId}/evaluations") @Operation(summary = "Evaluate an eligible completed workshop")
    public ResponseEntity<EvaluationResponse> create(@PathVariable UUID workshopId, @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey, @Valid @RequestBody CreateEvaluationRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(authentication), workshopId, idempotencyKey, request));
    }
    @GetMapping("/api/v1/workshops/{workshopId}/evaluations") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") @Operation(summary = "List evaluations for a managed workshop")
    public PageResponse<EvaluationResponse> list(@PathVariable UUID workshopId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, Authentication authentication) {
        return PageResponse.from(service.listForWorkshop(userId(authentication), admin(authentication), workshopId, pageable));
    }
    @GetMapping("/api/v1/workshops/{workshopId}/evaluations/summary") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") @Operation(summary = "Summarize evaluations for a managed workshop")
    public EvaluationSummaryResponse summary(@PathVariable UUID workshopId, Authentication authentication) { return service.summary(userId(authentication), admin(authentication), workshopId); }
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
    private boolean admin(Authentication authentication) { return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals); }
}
