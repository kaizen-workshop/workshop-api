package br.com.weg.workshop.evaluation.repository;

import br.com.weg.workshop.evaluation.domain.Evaluation;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
    boolean existsByUserIdAndWorkshopId(UUID userId, UUID workshopId);
    Page<Evaluation> findByWorkshopId(UUID workshopId, Pageable pageable);
    @Query("select avg(e.rating), avg(e.contentRating), avg(e.instructorRating), avg(e.organizationRating), count(e) from Evaluation e where e.workshop.id = :workshopId")
    Object[] summarize(@Param("workshopId") UUID workshopId);
}
