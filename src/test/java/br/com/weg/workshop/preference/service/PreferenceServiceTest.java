package br.com.weg.workshop.preference.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.preference.dto.ReplaceThemesRequest;
import br.com.weg.workshop.preference.repository.ThemeRepository;
import br.com.weg.workshop.preference.repository.UserThemeRepository;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {
    @Mock private UserRepository users;
    @Mock private ThemeRepository themes;
    @Mock private UserThemeRepository preferences;
    @InjectMocks private PreferenceService service;

    @Test
    void rejectsInactiveThemeWithoutReplacingExistingPreferences() {
        UserEntity user = user();
        Theme inactive = Theme.create("Inactive", null);
        inactive.update(null, null, false);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(themes.findAllById(Set.of(inactive.getId()))).thenReturn(java.util.List.of(inactive));

        assertThatThrownBy(() -> service.replace(user.getId(), new ReplaceThemesRequest(Set.of(inactive.getId()))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(preferences, never()).deleteByUserId(any());
        verify(preferences, never()).saveAll(any());
    }

    @Test
    void replacesPreferencesOnlyAfterAllThemeIdsAreValidated() {
        UserEntity user = user();
        Theme selected = Theme.create("Java", "Java workshops");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(themes.findAllById(Set.of(selected.getId()))).thenReturn(java.util.List.of(selected));

        service.replace(user.getId(), new ReplaceThemesRequest(Set.of(selected.getId())));

        verify(preferences).deleteByUserId(user.getId());
        verify(preferences).saveAll(any());
    }

    private UserEntity user() {
        return UserEntity.create("Jane Doe", "jane", "jane@example.com", null, null, null, "hash", Role.PARTICIPANT);
    }
}
