package br.com.weg.workshop.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.preference.service.PreferenceService;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.dto.UpdateProfileRequest;
import br.com.weg.workshop.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserRepository users;
    @Mock private PreferenceService preferences;
    @InjectMocks private ProfileService service;

    @Test
    void updatesOnlyPersonalProfileFields() {
        UserEntity user = UserEntity.create("Jane Doe", "jane", "jane@example.com", "123", "old", null, "hash", Role.PARTICIPANT);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(preferences.selectedThemes(user.getId())).thenReturn(List.of());

        var response = service.update(user.getId(), new UpdateProfileRequest("Jane Smith", "new", "image-key"));

        assertThat(response.name()).isEqualTo("Jane Smith");
        assertThat(response.phone()).isEqualTo("new");
        assertThat(response.username()).isEqualTo("jane");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(user.getRole()).isEqualTo(Role.PARTICIPANT);
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }
}
