package br.com.weg.workshop.chat.controller;

import br.com.weg.workshop.chat.dto.MessageRequest;
import br.com.weg.workshop.chat.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chat;

    public ChatWebSocketController(ChatService chat) {
        this.chat = chat;
    }

    @MessageMapping("/groups/{groupId}/messages")
    public void send(
            @DestinationVariable UUID groupId,
            @Valid MessageRequest request,
            Principal principal
    ) {
        Authentication authentication = (Authentication) principal;
        chat.send(
                UUID.fromString(authentication.getName()),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals),
                groupId,
                request
        );
    }
}
