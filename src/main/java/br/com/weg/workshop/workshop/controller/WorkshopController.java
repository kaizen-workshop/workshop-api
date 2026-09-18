package br.com.weg.workshop.workshop.controller;

import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import br.com.weg.workshop.workshop.dto.*;
import br.com.weg.workshop.workshop.service.WorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workshops")
public class WorkshopController {
 private final WorkshopService service;
 public WorkshopController(WorkshopService service){this.service=service;}
 @PostMapping @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") @Operation(summary="Create a workshop draft")
 public ResponseEntity<WorkshopResponse> create(@Valid @RequestBody WorkshopRequest request,Authentication authentication){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(authentication),request));}
 @GetMapping @Operation(summary="List workshops visible to the caller")
 public Page<WorkshopResponse> list(@RequestParam(required=false) WorkshopStatus status,@RequestParam(required=false) UUID themeId,@RequestParam(required=false) UUID categoryId,@PageableDefault(size=20,sort="startDate") Pageable pageable,Authentication authentication){return service.list(userId(authentication),admin(authentication),status,themeId,categoryId,pageable);}
 @GetMapping("/{id}") @Operation(summary="Get workshop details") public WorkshopResponse get(@PathVariable UUID id,Authentication authentication){return service.get(userId(authentication),admin(authentication),id);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") @Operation(summary="Update a draft or scheduled workshop") public WorkshopResponse update(@PathVariable UUID id,@Valid @RequestBody WorkshopRequest request,Authentication authentication){return service.update(userId(authentication),admin(authentication),id,request);}
 @PatchMapping("/{id}/schedule") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public WorkshopResponse schedule(@PathVariable UUID id,@Valid @RequestBody SchedulePublicationRequest request,Authentication authentication){return service.schedule(userId(authentication),admin(authentication),id,request);}
 @PatchMapping("/{id}/publish") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public WorkshopResponse publish(@PathVariable UUID id,Authentication authentication){return service.publish(userId(authentication),admin(authentication),id);}
 @PatchMapping("/{id}/close") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public WorkshopResponse close(@PathVariable UUID id,Authentication authentication){return service.close(userId(authentication),admin(authentication),id);}
 @PatchMapping("/{id}/cancel") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public WorkshopResponse cancel(@PathVariable UUID id,Authentication authentication){return service.cancel(userId(authentication),admin(authentication),id);}
 @PatchMapping("/{id}/archive") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public WorkshopResponse archive(@PathVariable UUID id,Authentication authentication){return service.archive(userId(authentication),admin(authentication),id);}
 @PostMapping("/{id}/duplicate") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')") public ResponseEntity<WorkshopResponse> duplicate(@PathVariable UUID id,Authentication authentication){return ResponseEntity.status(HttpStatus.CREATED).body(service.duplicate(userId(authentication),admin(authentication),id));}
 private UUID userId(Authentication authentication){return UUID.fromString(authentication.getName());}
 private boolean admin(Authentication authentication){return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);}
}
