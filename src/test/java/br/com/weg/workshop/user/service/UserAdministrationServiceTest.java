package br.com.weg.workshop.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.dto.CreateUserRequest;
import br.com.weg.workshop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserAdministrationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final InitialAccessMailService mail = mock(InitialAccessMailService.class);
    private final UserAdministrationService service = new UserAdministrationService(users, new BCryptPasswordEncoder(),
            new TemporaryPasswordGenerator(), mail);

    @Test
    void createsPendingUserAndSendsInitialAccessEmail() {
        CreateUserRequest request = request();
        when(users.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertThat(response.username()).isEqualTo("ana.silva");
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(response.status().name()).isEqualTo("PENDING");
        verify(mail).send(org.mockito.ArgumentMatchers.eq("ana@example.com"),
                org.mockito.ArgumentMatchers.eq("ana.silva"), any(String.class));
    }

    @Test
    void rejectsDuplicateUsernameBeforeSendingEmail() {
        when(users.existsByUsername("ana.silva")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(ConflictException.class);

        verify(users, never()).save(any());
        verify(mail, never()).send(any(), any(), any());
    }

    private CreateUserRequest request() {
        return new CreateUserRequest("Ana Silva", "ana.silva", "ana@example.com", "123", null, null, Role.PARTICIPANT);
    }
}
