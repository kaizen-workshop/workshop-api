package br.com.weg.workshop.group.repository;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkshopGroupRepository extends JpaRepository<WorkshopGroup, UUID> {
    Optional<WorkshopGroup> findByWorkshopId(UUID workshopId);

    @Query("""
            select distinct g from WorkshopGroup g
            join Registration r on r.workshop = g.workshop
            where r.user.id = :userId
              and r.status = br.com.weg.workshop.registration.domain.RegistrationStatus.CONFIRMED
              and r.paymentStatus in (
                br.com.weg.workshop.registration.domain.RegistrationPaymentStatus.PAID,
                br.com.weg.workshop.registration.domain.RegistrationPaymentStatus.EXEMPT
              )
            """)
    Page<WorkshopGroup> findAccessibleByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select distinct g from WorkshopGroup g
            left join Registration r on r.workshop = g.workshop
            where g.workshop.createdBy.id = :userId
               or (r.user.id = :userId
                   and r.status = br.com.weg.workshop.registration.domain.RegistrationStatus.CONFIRMED
                   and r.paymentStatus in (
                     br.com.weg.workshop.registration.domain.RegistrationPaymentStatus.PAID,
                     br.com.weg.workshop.registration.domain.RegistrationPaymentStatus.EXEMPT
                   ))
            """)
    Page<WorkshopGroup> findAccessibleOrManagedByUserId(@Param("userId") UUID userId, Pageable pageable);
}
