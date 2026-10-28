package br.com.weg.workshop.evaluation.dto;

import jakarta.validation.constraints.*;

public record CreateEvaluationRequest(@NotNull @Min(1) @Max(5) Short rating,
                                      @Size(max = 4000) String comment,
                                      @NotNull @Min(1) @Max(5) Short contentRating,
                                      @NotNull @Min(1) @Max(5) Short instructorRating,
                                      @NotNull @Min(1) @Max(5) Short organizationRating) { }
