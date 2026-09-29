package br.com.weg.workshop.notification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String message, boolean read,
                                   Map<String, String> data, Instant createdAt) { }
