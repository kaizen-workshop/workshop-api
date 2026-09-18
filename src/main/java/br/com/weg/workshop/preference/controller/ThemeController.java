package br.com.weg.workshop.preference.controller;

import br.com.weg.workshop.preference.dto.*;
import br.com.weg.workshop.preference.service.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ThemeController {
    private final ThemeService service;
    public ThemeController(ThemeService service) { this.service = service; }
    @GetMapping("/themes") @Operation(summary = "List active themes")
    public List<ThemeResponse> active() { return service.active(); }
    @PostMapping("/admin/themes") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Create a theme")
    public ResponseEntity<ThemeResponse> create(@Valid @RequestBody TaxonomyRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PatchMapping("/admin/themes/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Update a theme")
    public ThemeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaxonomyRequest request) { return service.update(id, request); }
}
