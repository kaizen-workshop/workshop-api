package br.com.weg.workshop.file.dto;

import br.com.weg.workshop.file.domain.WorkshopAttachment;
import java.time.Instant;
import java.util.UUID;

public record WorkshopFileResponse(UUID id, String filename, String contentType, long sizeBytes,
                                   String checksumSha256, Instant createdAt) {
    public static WorkshopFileResponse from(WorkshopAttachment attachment) {
        return new WorkshopFileResponse(attachment.getId(), attachment.getOriginalFilename(), attachment.getContentType(),
                attachment.getSizeBytes(), attachment.getChecksumSha256(), attachment.getCreatedAt());
    }
}
