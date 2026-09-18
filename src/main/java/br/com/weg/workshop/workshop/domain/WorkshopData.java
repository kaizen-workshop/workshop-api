package br.com.weg.workshop.workshop.domain;
import java.math.BigDecimal; import java.time.*;
public record WorkshopData(String title, String description, String image, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String location, WorkshopModality modality, BigDecimal price, Instant registrationStart, Instant registrationEnd, int maximumParticipants, PaymentMethod paymentMethod, String additionalInformation) { }
