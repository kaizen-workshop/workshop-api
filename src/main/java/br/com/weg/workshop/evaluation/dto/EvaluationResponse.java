package br.com.weg.workshop.evaluation.dto;

import br.com.weg.workshop.evaluation.domain.Evaluation;
import java.time.Instant;
import java.util.UUID;

public record EvaluationResponse(UUID id, UUID workshopId, short rating, String comment, short contentRating,
                                 short instructorRating, short organizationRating, Instant createdAt, Instant updatedAt) {
    public static EvaluationResponse from(Evaluation value) { return new EvaluationResponse(value.getId(), value.getWorkshop().getId(), value.getRating(), value.getComment(), value.getContentRating(), value.getInstructorRating(), value.getOrganizationRating(), value.getCreatedAt(), value.getUpdatedAt()); }
}
