package br.com.weg.workshop.file.domain;

import br.com.weg.workshop.workshop.domain.Workshop;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workshop_attachment", schema = "workshop")
public class WorkshopAttachment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkshopFileType type;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 10)
    private String extension;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Column(nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkshopAttachment() {
    }

    public static WorkshopAttachment create(Workshop workshop, WorkshopFileType type, String originalFilename,
                                             String contentType, String extension, long sizeBytes,
                                             String checksumSha256, String storageKey) {
        WorkshopAttachment attachment = new WorkshopAttachment();
        attachment.id = UUID.randomUUID();
        attachment.workshop = workshop;
        attachment.type = type;
        attachment.originalFilename = originalFilename;
        attachment.contentType = contentType;
        attachment.extension = extension;
        attachment.sizeBytes = sizeBytes;
        attachment.checksumSha256 = checksumSha256;
        attachment.storageKey = storageKey;
        attachment.createdAt = Instant.now();
        return attachment;
    }

    public UUID getId() { return id; }
    public Workshop getWorkshop() { return workshop; }
    public WorkshopFileType getType() { return type; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public String getExtension() { return extension; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksumSha256() { return checksumSha256; }
    public String getStorageKey() { return storageKey; }
    public Instant getCreatedAt() { return createdAt; }
}
