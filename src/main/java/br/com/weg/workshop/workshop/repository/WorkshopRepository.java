package br.com.weg.workshop.workshop.repository;
import br.com.weg.workshop.workshop.domain.*; import java.time.Instant; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface WorkshopRepository extends JpaRepository<Workshop,UUID> {
 @Query("select w from Workshop w where (w.status = 'PUBLISHED' or w.createdBy.id = :userId or :admin = true) and (:status is null or w.status = :status) and (:themeId is null or w.theme.id = :themeId) and (:categoryId is null or w.category.id = :categoryId)")
 Page<Workshop> findVisible(@Param("userId") UUID userId,@Param("admin") boolean admin,@Param("status") WorkshopStatus status,@Param("themeId") UUID themeId,@Param("categoryId") UUID categoryId, Pageable pageable);
 List<Workshop> findByStatusAndScheduledPublishAtLessThanEqual(WorkshopStatus status, Instant instant);
}
