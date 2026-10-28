package br.com.weg.workshop.preference.service;

import br.com.weg.workshop.preference.domain.UserTheme;
import br.com.weg.workshop.preference.dto.ReplaceThemesRequest;
import br.com.weg.workshop.preference.dto.ThemeResponse;
import br.com.weg.workshop.preference.repository.ThemeRepository;
import br.com.weg.workshop.preference.repository.UserThemeRepository;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {
    private final UserRepository users; private final ThemeRepository themes; private final UserThemeRepository preferences;
    public PreferenceService(UserRepository users, ThemeRepository themes, UserThemeRepository preferences) { this.users = users; this.themes = themes; this.preferences = preferences; }
    @Transactional public void replace(UUID userId, ReplaceThemesRequest request) {
        var user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        var selected = themes.findAllById(request.themeIds());
        if (selected.size() != request.themeIds().size() || selected.stream().anyMatch(theme -> !theme.isActive())) throw new IllegalArgumentException("Themes must exist and be active.");
        preferences.deleteByUserId(userId);
        preferences.saveAll(selected.stream().map(theme -> new UserTheme(user, theme)).toList());
    }
    @Transactional(readOnly = true) public List<ThemeResponse> selectedThemes(UUID userId) { return preferences.findByUserId(userId).stream().map(UserTheme::getTheme).map(ThemeResponse::from).toList(); }
}
