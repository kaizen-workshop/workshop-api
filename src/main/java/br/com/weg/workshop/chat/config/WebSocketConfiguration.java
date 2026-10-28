package br.com.weg.workshop.chat.config;

import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.user.domain.UserStatus;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwt;
    private final GroupService groups;
    private final UserRepository users;
    private final List<String> allowedOrigins;

    public WebSocketConfiguration(
            JwtService jwt,
            GroupService groups,
            UserRepository users,
            @Value("${app.security.cors.allowed-origins:http://localhost:8081,http://127.0.0.1:8081}")
            List<String> allowedOrigins
    ) {
        this.jwt = jwt;
        this.groups = groups;
        this.users = users;
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) authenticate(accessor);
                if (StompCommand.SEND.equals(accessor.getCommand())
                        || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    requireCurrentAccount(accessor);
                }
                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination == null || !destination.matches("/app/groups/[0-9a-fA-F-]{36}/messages")) {
                        throw new IllegalArgumentException("Invalid message destination.");
                    }
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) authorizeSubscription(accessor);
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        Claims claims = jwt.parse(authorization.substring(7));
        var user = users.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid authentication."));
        Object version = claims.get("tokenVersion");
        if (!(version instanceof Number number) || number.intValue() != user.getTokenVersion()
                || !user.getRole().name().equals(claims.get("role", String.class))
                || user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Invalid authentication.");
        }
        if (user.isMustChangePassword()) {
            throw new IllegalArgumentException("Password change is required.");
        }
        String role = user.getRole().name();
        var authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        authentication.setDetails(user.getTokenVersion());
        accessor.setUser(authentication);
    }

    private void requireCurrentAccount(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getDetails() instanceof Integer version)) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        var user = users.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Authentication is required."));
        String role = "ROLE_" + user.getRole().name();
        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.INACTIVE
                || user.isMustChangePassword() || user.getTokenVersion() != version
                || authentication.getAuthorities().stream().noneMatch(authority -> role.equals(authority.getAuthority()))) {
            throw new IllegalArgumentException("Authentication is required.");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        String destination = accessor.getDestination();
        String prefix = "/topic/groups/";
        if (destination == null || !destination.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid subscription destination.");
        }
        UUID groupId = UUID.fromString(destination.substring(prefix.length()));
        boolean admin = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch("ROLE_ADMIN"::equals);
        groups.requireAccess(UUID.fromString(authentication.getName()), admin, groupId);
    }
}
