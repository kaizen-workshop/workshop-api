package br.com.weg.workshop.workshop.dto;
import br.com.weg.workshop.workshop.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.*; import java.util.UUID;
public record WorkshopRequest(@NotBlank @Size(max=180) String title, @NotBlank String description, @Size(max=500) String image, @NotNull UUID themeId, @NotNull UUID categoryId, @NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @NotBlank @Size(max=250) String location, @NotNull WorkshopModality modality, @NotNull @DecimalMin("0.0") BigDecimal price, @NotNull Instant registrationStart, @NotNull Instant registrationEnd, @Positive int maximumParticipants, @NotNull PaymentMethod paymentMethod, boolean championship, String additionalInformation) { }
