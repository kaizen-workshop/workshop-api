package br.com.weg.workshop.notification.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ManualNotificationRequest(
        @NotEmpty Set<UUID> userIds,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4000) String message,
        Map<String, String> data,
        @FutureOrPresent Instant scheduledAt
) { }
