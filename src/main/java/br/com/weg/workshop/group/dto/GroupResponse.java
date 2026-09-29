package br.com.weg.workshop.group.dto;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        UUID workshopId,
        String workshopTitle,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupResponse from(WorkshopGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getWorkshop().getId(),
                group.getWorkshop().getTitle(),
                group.isActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
