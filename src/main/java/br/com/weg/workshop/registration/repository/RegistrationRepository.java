package br.com.weg.workshop.registration.repository;

import br.com.weg.workshop.registration.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    boolean existsByUserIdAndWorkshopIdAndStatusIn(UUID userId, UUID workshopId, Collection<RegistrationStatus> statuses);

    long countByWorkshopIdAndStatusIn(UUID workshopId, Collection<RegistrationStatus> statuses);

    @Query("select r from Registration r join r.user u where r.workshop.id = :workshopId and r.status = 'WAITING_LIST' "
            + "and u.status = 'ACTIVE' order by r.registeredAt asc, r.id asc")
    Optional<Registration> findFirstEligibleWaitingListEntry(@Param("workshopId") UUID workshopId);

    @Query("select r from Registration r where r.workshop.id = :workshopId and (:status is null or r.status = :status)")
    Page<Registration> findByWorkshop(@Param("workshopId") UUID workshopId, @Param("status") RegistrationStatus status, Pageable pageable);
}
