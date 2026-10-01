package br.com.weg.workshop.audit.service;

import br.com.weg.workshop.audit.domain.AdministrativeAudit;
import br.com.weg.workshop.audit.dto.AuditResponse;
import br.com.weg.workshop.audit.repository.AdministrativeAuditRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private final AdministrativeAuditRepository audits;

    public AuditService(AdministrativeAuditRepository audits) { this.audits = audits; }

    @Transactional
    public void record(UUID actorId, String action, String entity, UUID entityId,
                       String previousValue, String newValue) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        String ip = attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getRemoteAddr() : null;
        audits.save(new AdministrativeAudit(actorId, action, entity, entityId, previousValue, newValue, ip));
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> search(UUID userId, String action, String entity, UUID entityId,
                                      Instant from, Instant to, Pageable pageable) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("The end of the audit period must be after its start.");
        }
        return audits.search(userId, action, entity, entityId, from, to, pageable).map(AuditResponse::from);
    }
}
