package br.com.weg.workshop.preference.service;

import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.preference.dto.*;
import br.com.weg.workshop.preference.repository.ThemeRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThemeService {
    private final ThemeRepository themes;
    public ThemeService(ThemeRepository themes) { this.themes = themes; }
    @Transactional public ThemeResponse create(TaxonomyRequest request) {
        if (themes.existsByName(request.name())) throw new ConflictException("Theme name already exists.");
        return ThemeResponse.from(themes.save(Theme.create(request.name(), request.description())));
    }
    @Transactional(readOnly = true) public List<ThemeResponse> active() { return themes.findByActiveTrue().stream().map(ThemeResponse::from).toList(); }
    @Transactional public ThemeResponse update(UUID id, UpdateTaxonomyRequest request) {
        if (!request.hasChanges()) throw new IllegalArgumentException("At least one taxonomy field must be provided.");
        Theme theme = themes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
        if (request.name() != null && !theme.getName().equals(request.name()) && themes.existsByName(request.name())) throw new ConflictException("Theme name already exists.");
        theme.update(request.name(), request.description(), request.active());
        return ThemeResponse.from(theme);
    }
}
