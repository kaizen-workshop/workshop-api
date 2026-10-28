package br.com.weg.workshop.audit.repository;

import br.com.weg.workshop.audit.domain.AdministrativeAudit;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdministrativeAuditRepository extends JpaRepository<AdministrativeAudit, UUID> {
    @Query("select a from AdministrativeAudit a where (:userId is null or a.userId = :userId) "
            + "and (:action is null or a.action = :action) and (:entity is null or a.entity = :entity) "
            + "and (:entityId is null or a.entityId = :entityId) "
            + "and (cast(:from as timestamp) is null or a.timestamp >= :from) "
            + "and (cast(:to as timestamp) is null or a.timestamp < :to)")
    Page<AdministrativeAudit> search(@Param("userId") UUID userId, @Param("action") String action,
                                     @Param("entity") String entity, @Param("entityId") UUID entityId,
                                     @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
