package br.com.weg.workshop.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.preference.dto.TaxonomyRequest;
import br.com.weg.workshop.preference.dto.UpdateTaxonomyRequest;
import br.com.weg.workshop.preference.repository.ThemeRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {
    @Mock private ThemeRepository themes;
    @InjectMocks private ThemeService service;

    @Test
    void rejectsDuplicateNameDuringCreation() {
        when(themes.existsByName("Java")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new TaxonomyRequest("Java", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatesMetadataAndActiveState() {
        Theme theme = Theme.create("Java", "Old description");
        when(themes.findById(theme.getId())).thenReturn(Optional.of(theme));

        var response = service.update(theme.getId(), new UpdateTaxonomyRequest("Cloud", "New description", false));

        assertThat(response.name()).isEqualTo("Cloud");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.active()).isFalse();
    }

    @Test
    void returnsNotFoundForUnknownTheme() {
        var id = java.util.UUID.randomUUID();
        when(themes.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id, new UpdateTaxonomyRequest(null, null, false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
