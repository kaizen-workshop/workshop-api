package br.com.weg.workshop.file.service;

import br.com.weg.workshop.file.domain.*;
import br.com.weg.workshop.file.dto.WorkshopFileResponse;
import br.com.weg.workshop.file.repository.WorkshopAttachmentRepository;
import br.com.weg.workshop.file.storage.FileStorage;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.workshop.domain.Workshop;
import br.com.weg.workshop.workshop.service.WorkshopService;
import java.io.*;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkshopMediaService {

    private final WorkshopService workshops;
    private final WorkshopAttachmentRepository attachments;
    private final FileStorage storage;
    private final FileUploadValidator validator;

    public WorkshopMediaService(WorkshopService workshops, WorkshopAttachmentRepository attachments, FileStorage storage,
                                FileUploadValidator validator) {
        this.workshops = workshops;
        this.attachments = attachments;
        this.storage = storage;
        this.validator = validator;
    }

    @Transactional
    public WorkshopFileResponse uploadImage(UUID userId, boolean admin, UUID workshopId, MultipartFile file) {
        Workshop workshop = workshops.manageableForMedia(userId, admin, workshopId);
        ValidatedUpload upload = validator.validate(file);
        WorkshopAttachment previous = attachments.findByWorkshopIdAndType(workshopId, WorkshopFileType.IMAGE).orElse(null);
        WorkshopAttachment attachment = store(workshop, WorkshopFileType.IMAGE, upload);
        if (previous != null) {
            attachments.delete(previous);
            attachments.flush();
        }
        attachments.save(attachment);
        workshop.replaceImage(attachment.getStorageKey());
        if (previous != null) {
            deleteQuietly(previous.getStorageKey());
        }
        return WorkshopFileResponse.from(attachment);
    }

    @Transactional
    public WorkshopFileResponse uploadAttachment(UUID userId, boolean admin, UUID workshopId, MultipartFile file) {
        Workshop workshop = workshops.manageableForMedia(userId, admin, workshopId);
        WorkshopAttachment attachment = store(workshop, WorkshopFileType.ATTACHMENT, validator.validate(file));
        attachments.save(attachment);
        return WorkshopFileResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public List<WorkshopFileResponse> listAttachments(UUID userId, boolean admin, UUID workshopId) {
        workshops.visibleForMedia(userId, admin, workshopId);
        return attachments.findAllByWorkshopIdAndTypeOrderByCreatedAtAsc(workshopId, WorkshopFileType.ATTACHMENT).stream()
                .map(WorkshopFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoredWorkshopFile downloadImage(UUID userId, boolean admin, UUID workshopId) {
        return download(userId, admin, workshopId, null, WorkshopFileType.IMAGE);
    }

    @Transactional(readOnly = true)
    public StoredWorkshopFile downloadAttachment(UUID userId, boolean admin, UUID workshopId, UUID attachmentId) {
        return download(userId, admin, workshopId, attachmentId, WorkshopFileType.ATTACHMENT);
    }

    @Transactional
    public void deleteImage(UUID userId, boolean admin, UUID workshopId) {
        Workshop workshop = workshops.manageableForMedia(userId, admin, workshopId);
        WorkshopAttachment attachment = attachments.findByWorkshopIdAndType(workshopId, WorkshopFileType.IMAGE)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop image not found."));
        deleteStored(attachment);
        attachments.delete(attachment);
        workshop.removeImage();
    }

    @Transactional
    public void deleteAttachment(UUID userId, boolean admin, UUID workshopId, UUID attachmentId) {
        workshops.manageableForMedia(userId, admin, workshopId);
        WorkshopAttachment attachment = attachments.findByIdAndWorkshopIdAndType(attachmentId, workshopId, WorkshopFileType.ATTACHMENT)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop attachment not found."));
        deleteStored(attachment);
        attachments.delete(attachment);
    }

    private WorkshopAttachment store(Workshop workshop, WorkshopFileType type, ValidatedUpload upload) {
        String storageKey = "workshops/" + workshop.getId() + "/" + UUID.randomUUID() + "." + upload.extension();
        try {
            storage.store(storageKey, new ByteArrayInputStream(upload.content()));
        } catch (IOException exception) {
            throw new FileStorageException();
        }
        return WorkshopAttachment.create(workshop, type, upload.filename(), upload.contentType(), upload.extension(),
                upload.content().length, upload.checksumSha256(), storageKey);
    }

    private StoredWorkshopFile download(UUID userId, boolean admin, UUID workshopId, UUID attachmentId, WorkshopFileType type) {
        workshops.visibleForMedia(userId, admin, workshopId);
        WorkshopAttachment attachment = attachmentId == null
                ? attachments.findByWorkshopIdAndType(workshopId, type).orElseThrow(() -> new ResourceNotFoundException("Workshop image not found."))
                : attachments.findByIdAndWorkshopIdAndType(attachmentId, workshopId, type)
                        .orElseThrow(() -> new ResourceNotFoundException("Workshop attachment not found."));
        try {
            Resource resource = storage.load(attachment.getStorageKey());
            return new StoredWorkshopFile(attachment, resource);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Stored file not found.");
        }
    }

    private void deleteStored(WorkshopAttachment attachment) {
        try {
            storage.delete(attachment.getStorageKey());
        } catch (IOException exception) {
            throw new FileStorageException();
        }
    }

    private void deleteQuietly(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (IOException ignored) {
            // The prior file is already inaccessible after replacement; cleanup can be retried operationally.
        }
    }
}
