package br.com.weg.workshop.audit.dto;

import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkshopMetricResponse(UUID id, String title, WorkshopStatus status, Instant createdAt,
                                     long registrations, long confirmed, long attended) { }
