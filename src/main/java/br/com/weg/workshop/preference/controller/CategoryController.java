package br.com.weg.workshop.preference.controller;

import br.com.weg.workshop.preference.dto.*;
import br.com.weg.workshop.preference.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @GetMapping("/categories") @Operation(summary = "List active categories")
    public List<CategoryResponse> active() { return service.active(); }
    @PostMapping("/admin/categories") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Create a category")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody TaxonomyRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PatchMapping("/admin/categories/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary = "Update a category")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaxonomyRequest request) { return service.update(id, request); }
}
