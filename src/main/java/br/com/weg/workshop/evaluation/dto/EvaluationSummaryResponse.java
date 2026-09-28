package br.com.weg.workshop.evaluation.dto;

import java.util.UUID;

public record EvaluationSummaryResponse(UUID workshopId, long total, Double averageRating, Double averageContentRating,
                                        Double averageInstructorRating, Double averageOrganizationRating) { }
