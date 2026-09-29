package br.com.weg.workshop.chat.controller;

import br.com.weg.workshop.chat.dto.MessagePageResponse;
import br.com.weg.workshop.chat.dto.MessageRequest;
import br.com.weg.workshop.chat.dto.MessageResponse;
import br.com.weg.workshop.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/groups/{groupId}/messages")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    @Operation(summary = "List group messages using a stable cursor")
    public MessagePageResponse list(
            @PathVariable UUID groupId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        return chat.list(userId(authentication), admin(authentication), groupId, cursor, size);
    }

    @PostMapping
    @Operation(summary = "Persist and deliver a group message")
    public ResponseEntity<MessageResponse> send(
            @PathVariable UUID groupId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chat.send(userId(authentication), admin(authentication), groupId, request));
    }

    @PatchMapping("/{messageId}")
    @Operation(summary = "Edit the caller's message")
    public MessageResponse edit(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication
    ) {
        return chat.edit(userId(authentication), admin(authentication), groupId, messageId, request);
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Soft-delete an own or moderated message")
    public MessageResponse delete(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        return chat.delete(userId(authentication), admin(authentication), groupId, messageId);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
