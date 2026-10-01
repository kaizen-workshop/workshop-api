package br.com.weg.workshop.audit.dto;

import br.com.weg.workshop.audit.domain.AdministrativeAudit;
import java.time.Instant;
import java.util.UUID;

public record AuditResponse(UUID id, UUID userId, String action, String entity, UUID entityId,
                            String previousValue, String newValue, Instant timestamp, String ip) {
    public static AuditResponse from(AdministrativeAudit audit) {
        return new AuditResponse(audit.getId(), audit.getUserId(), audit.getAction(), audit.getEntity(),
                audit.getEntityId(), audit.getPreviousValue(), audit.getNewValue(), audit.getTimestamp(), audit.getIp());
    }
}
