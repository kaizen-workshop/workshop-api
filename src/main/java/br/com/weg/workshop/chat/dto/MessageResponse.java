package br.com.weg.workshop.chat.dto;

import br.com.weg.workshop.chat.domain.Message;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID groupId,
        UUID authorId,
        String authorName,
        String content,
        Instant sentAt,
        Instant editedAt,
        Instant deletedAt
) {
    public static MessageResponse from(Message message) {
        boolean deleted = message.getDeletedAt() != null;
        return new MessageResponse(
                message.getId(),
                message.getGroup().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getName(),
                deleted ? null : message.getContent(),
                message.getSentAt(),
                message.getEditedAt(),
                message.getDeletedAt()
        );
    }
}
