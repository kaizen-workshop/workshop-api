package br.com.weg.workshop.preference.dto;
import br.com.weg.workshop.preference.domain.Category;
import java.util.UUID;
public record CategoryResponse(UUID id, String name, String description, boolean active) {
    public static CategoryResponse from(Category category) { return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.isActive()); }
}
