package br.com.weg.workshop.file.service;

import br.com.weg.workshop.file.domain.WorkshopAttachment;
import org.springframework.core.io.Resource;

public record StoredWorkshopFile(WorkshopAttachment attachment, Resource resource) {
}
