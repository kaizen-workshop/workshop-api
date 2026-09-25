package br.com.weg.workshop.file.repository;

import br.com.weg.workshop.file.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopAttachmentRepository extends JpaRepository<WorkshopAttachment, UUID> {
    Optional<WorkshopAttachment> findByWorkshopIdAndType(UUID workshopId, WorkshopFileType type);
    List<WorkshopAttachment> findAllByWorkshopIdAndTypeOrderByCreatedAtAsc(UUID workshopId, WorkshopFileType type);
    Optional<WorkshopAttachment> findByIdAndWorkshopIdAndType(UUID id, UUID workshopId, WorkshopFileType type);
}
