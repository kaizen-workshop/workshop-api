package br.com.weg.workshop.workshop.dto;
import jakarta.validation.constraints.Future; import jakarta.validation.constraints.NotNull; import java.time.Instant;
public record SchedulePublicationRequest(@NotNull @Future Instant scheduledPublishAt) { }
