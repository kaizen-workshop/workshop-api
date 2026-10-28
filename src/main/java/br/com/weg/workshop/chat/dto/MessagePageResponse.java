package br.com.weg.workshop.chat.dto;

import java.util.List;
import java.util.UUID;

public record MessagePageResponse(
        List<MessageResponse> content,
        UUID nextCursor,
        boolean hasMore
) {
}
