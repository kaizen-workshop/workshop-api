package br.com.weg.workshop.group.dto;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        UUID workshopId,
        String workshopTitle,
        boolean active,
        boolean canSendMessages,
        boolean canModerate,
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupResponse from(WorkshopGroup group, boolean canModerate) {
        return new GroupResponse(
                group.getId(),
                group.getWorkshop().getId(),
                group.getWorkshop().getTitle(),
                group.isActive(),
                group.isActive(),
                canModerate,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
