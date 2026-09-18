package br.com.weg.workshop.preference.dto;
import br.com.weg.workshop.preference.domain.Theme;
import java.util.UUID;
public record ThemeResponse(UUID id, String name, String description, boolean active) {
    public static ThemeResponse from(Theme theme) { return new ThemeResponse(theme.getId(), theme.getName(), theme.getDescription(), theme.isActive()); }
}
