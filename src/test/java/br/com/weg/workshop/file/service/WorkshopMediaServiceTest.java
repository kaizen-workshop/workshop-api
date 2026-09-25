package br.com.weg.workshop.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.file.domain.*;
import br.com.weg.workshop.file.repository.WorkshopAttachmentRepository;
import br.com.weg.workshop.file.storage.FileStorage;
import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.service.WorkshopService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class WorkshopMediaServiceTest {

    @Mock WorkshopService workshops;
    @Mock WorkshopAttachmentRepository attachments;
    @Mock FileStorage storage;
    @Spy FileUploadValidator validator = new FileUploadValidator();
    @InjectMocks WorkshopMediaService service;

    @Test
    void storesOnlyMetadataForAnAttachment() throws IOException {
        Workshop workshop = workshop();
        when(workshops.manageableForMedia(workshop.getCreatedBy().getId(), false, workshop.getId())).thenReturn(workshop);
        when(attachments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.uploadAttachment(workshop.getCreatedBy().getId(), false, workshop.getId(), pngFile());

        assertThat(response.filename()).isEqualTo("guide.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(9);
        verify(storage).store(startsWith("workshops/" + workshop.getId() + "/"), any());
        verify(attachments).save(argThat(attachment -> attachment.getType() == WorkshopFileType.ATTACHMENT
                && attachment.getStorageKey().endsWith(".png") && attachment.getChecksumSha256().length() == 64));
    }

    @Test
    void replacingImageRemovesPriorMetadataAndStorageObject() throws IOException {
        Workshop workshop = workshop();
        WorkshopAttachment previous = WorkshopAttachment.create(workshop, WorkshopFileType.IMAGE, "old.png", "image/png", "png", 9,
                "0".repeat(64), "workshops/old.png");
        when(workshops.manageableForMedia(workshop.getCreatedBy().getId(), false, workshop.getId())).thenReturn(workshop);
        when(attachments.findByWorkshopIdAndType(workshop.getId(), WorkshopFileType.IMAGE)).thenReturn(Optional.of(previous));
        when(attachments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.uploadImage(workshop.getCreatedBy().getId(), false, workshop.getId(), pngFile());

        verify(attachments).delete(previous);
        verify(attachments).flush();
        verify(storage).delete("workshops/old.png");
        assertThat(workshop.getImage()).startsWith("workshops/" + workshop.getId() + "/");
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "guide.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0x00});
    }

    private Workshop workshop() {
        UserEntity creator = UserEntity.create("ARWEG", "arweg", "arweg@example.com", null, null, null, "hash", Role.ARWEG);
        Theme theme = Theme.create("Java", null);
        Category category = Category.create("Technology", null);
        return Workshop.create(new WorkshopData("Workshop", "Description", null, LocalDate.now().plusDays(3), LocalDate.now().plusDays(3),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON, BigDecimal.ZERO,
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, PaymentMethod.FREE, false, null), theme, category, creator);
    }
}
