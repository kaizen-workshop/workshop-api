package br.com.weg.workshop.file.controller;

import br.com.weg.workshop.file.dto.WorkshopFileResponse;
import br.com.weg.workshop.file.service.*;
import io.swagger.v3.oas.annotations.Operation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/workshops/{workshopId}")
public class WorkshopMediaController {

    private final WorkshopMediaService service;

    public WorkshopMediaController(WorkshopMediaService service) {
        this.service = service;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Upload or replace a workshop image")
    public WorkshopFileResponse uploadImage(@PathVariable UUID workshopId, @RequestPart("file") MultipartFile file,
                                            Authentication authentication) {
        return service.uploadImage(userId(authentication), admin(authentication), workshopId, file);
    }

    @DeleteMapping("/image")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Delete a workshop image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID workshopId, Authentication authentication) {
        service.deleteImage(userId(authentication), admin(authentication), workshopId);
    }

    @GetMapping("/image/content")
    @Operation(summary = "Download a workshop image")
    public ResponseEntity<Resource> downloadImage(@PathVariable UUID workshopId, Authentication authentication) {
        return response(service.downloadImage(userId(authentication), admin(authentication), workshopId));
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Upload a workshop attachment")
    public ResponseEntity<WorkshopFileResponse> uploadAttachment(@PathVariable UUID workshopId, @RequestPart("file") MultipartFile file,
                                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.uploadAttachment(userId(authentication), admin(authentication), workshopId, file));
    }

    @GetMapping("/attachments")
    @Operation(summary = "List workshop attachments")
    public List<WorkshopFileResponse> listAttachments(@PathVariable UUID workshopId, Authentication authentication) {
        return service.listAttachments(userId(authentication), admin(authentication), workshopId);
    }

    @GetMapping("/attachments/{attachmentId}/content")
    @Operation(summary = "Download a workshop attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID workshopId, @PathVariable UUID attachmentId,
                                                        Authentication authentication) {
        return response(service.downloadAttachment(userId(authentication), admin(authentication), workshopId, attachmentId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Delete a workshop attachment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable UUID workshopId, @PathVariable UUID attachmentId, Authentication authentication) {
        service.deleteAttachment(userId(authentication), admin(authentication), workshopId, attachmentId);
    }

    private ResponseEntity<Resource> response(StoredWorkshopFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.attachment().getContentType()))
                .contentLength(file.attachment().getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.attachment().getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
    }
}
