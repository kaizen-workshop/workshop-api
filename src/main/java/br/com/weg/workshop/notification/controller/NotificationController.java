package br.com.weg.workshop.notification.controller;

import br.com.weg.workshop.notification.dto.*;
import br.com.weg.workshop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping("/notifications") @Operation(summary = "List the caller's notifications")
    public Page<NotificationResponse> list(@RequestParam(required = false) Instant updatedAfter,
            @PageableDefault(size = 20, sort = "updatedAt",
            direction = Sort.Direction.DESC) Pageable pageable, Authentication authentication) {
        return notifications.list(userId(authentication), updatedAfter, pageable);
    }
    @PatchMapping("/notifications/{id}/read") @Operation(summary = "Mark an owned notification as read")
    public NotificationResponse read(@PathVariable UUID id, Authentication authentication) {
        return notifications.markRead(userId(authentication), id);
    }
    @PatchMapping("/notifications/read-all") @Operation(summary = "Mark all caller notifications as read")
    public ResponseEntity<Void> readAll(Authentication authentication) {
        notifications.markAllRead(userId(authentication)); return ResponseEntity.noContent().build();
    }
    @PostMapping("/notification-devices") @Operation(summary = "Register or reactivate a push device")
    public ResponseEntity<DeviceResponse> register(@Valid @RequestBody RegisterDeviceRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notifications.registerDevice(userId(authentication), request));
    }
    @DeleteMapping("/notification-devices/{id}") @Operation(summary = "Deactivate an owned push device")
    public ResponseEntity<Void> unregister(@PathVariable UUID id, Authentication authentication) {
        notifications.unregisterDevice(userId(authentication), id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/arweg/notifications") @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    @Operation(summary = "Create or schedule manual notifications")
    public ResponseEntity<List<NotificationResponse>> manual(@Valid @RequestBody ManualNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notifications.createManual(request));
    }
    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }
}
