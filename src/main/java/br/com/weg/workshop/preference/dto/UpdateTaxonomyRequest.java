package br.com.weg.workshop.preference.dto;

import jakarta.validation.constraints.Size;

public record UpdateTaxonomyRequest(@Size(min = 1, max = 120) String name, @Size(max = 500) String description, Boolean active) {
    public boolean hasChanges() { return name != null || description != null || active != null; }
}
