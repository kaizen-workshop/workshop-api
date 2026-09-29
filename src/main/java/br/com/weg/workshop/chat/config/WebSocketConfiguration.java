package br.com.weg.workshop.chat.config;

import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.group.service.GroupService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
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

    public WebSocketConfiguration(JwtService jwt, GroupService groups) {
        this.jwt = jwt;
        this.groups = groups;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) authenticate(accessor);
                if (StompCommand.SEND.equals(accessor.getCommand()) && accessor.getUser() == null) {
                    throw new IllegalArgumentException("Authentication is required.");
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) authorizeSubscription(accessor);
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authentication is required.");
        }
        Claims claims = jwt.parse(authorization.substring(7));
        if (Boolean.TRUE.equals(claims.get("mustChangePassword", Boolean.class))) {
            throw new IllegalArgumentException("Password change is required.");
        }
        String role = claims.get("role", String.class);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
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
