package br.com.weg.workshop.chat.config;

import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketConfigurationTest {
    private final JwtService jwt = mock(JwtService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WebSocketConfiguration configuration = new WebSocketConfiguration(jwt, mock(GroupService.class), users);

    @Test void connectPreservesAuthenticatedPrincipal() {
        var user = UserEntity.create("Person", "person", "person@example.com", null, null, null, "hash", Role.PARTICIPANT);
        user.changePassword("new-hash");
        when(jwt.parse("token")).thenReturn(Jwts.claims().subject(user.getId().toString())
                .add("role", "PARTICIPANT").add("tokenVersion", user.getTokenVersion()).build());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.addNativeHeader("Authorization", "Bearer token");
        var result = interceptor().preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
        assertThat(StompHeaderAccessor.wrap(result).getUser().getName()).isEqualTo(user.getId().toString());
    }

    @Test void clientsCannotSendDirectlyToBrokerAndBypassChatAuthorization() {
        var headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/topic/groups/123");
        headers.setUser(new UsernamePasswordAuthenticationToken("user", null));
        assertThatThrownBy(() -> interceptor().preSend(
                MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid message destination.");
    }

    private ChannelInterceptor interceptor() {
        var registration = mock(ChannelRegistration.class);
        var captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        configuration.configureClientInboundChannel(registration);
        verify(registration).interceptors(captor.capture());
        return captor.getValue();
    }
}
