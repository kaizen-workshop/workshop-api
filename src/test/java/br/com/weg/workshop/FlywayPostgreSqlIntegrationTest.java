package br.com.weg.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.weg.workshop.audit.service.AuditService;
import br.com.weg.workshop.audit.service.MetricService;
import br.com.weg.workshop.notification.domain.NotificationType;
import br.com.weg.workshop.notification.service.NotificationService;
import br.com.weg.workshop.post.domain.Post;
import br.com.weg.workshop.post.domain.PostComment;
import br.com.weg.workshop.post.domain.PostLike;
import br.com.weg.workshop.post.domain.PostStatus;
import br.com.weg.workshop.post.repository.PostCommentRepository;
import br.com.weg.workshop.post.repository.PostLikeRepository;
import br.com.weg.workshop.post.repository.PostRepository;
import br.com.weg.workshop.post.service.PostService;
import br.com.weg.workshop.preference.domain.Category;
import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.preference.repository.CategoryRepository;
import br.com.weg.workshop.preference.repository.ThemeRepository;
import br.com.weg.workshop.registration.domain.AttendanceStatus;
import br.com.weg.workshop.registration.domain.Registration;
import br.com.weg.workshop.registration.domain.RegistrationPaymentStatus;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.PaymentMethod;
import br.com.weg.workshop.workshop.domain.Workshop;
import br.com.weg.workshop.workshop.domain.WorkshopData;
import br.com.weg.workshop.workshop.domain.WorkshopModality;
import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import br.com.weg.workshop.workshop.service.WorkshopService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class FlywayPostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MetricService metrics;

    @Autowired
    private AuditService audit;

    @Autowired private UserRepository users;
    @Autowired private ThemeRepository themes;
    @Autowired private CategoryRepository categories;
    @Autowired private WorkshopRepository workshops;
    @Autowired private RegistrationRepository registrations;
    @Autowired private PostRepository posts;
    @Autowired private PostLikeRepository likes;
    @Autowired private PostCommentRepository comments;
    @Autowired private WorkshopService workshopService;
    @Autowired private PostService postService;
    @Autowired private NotificationService notificationService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void flywayAppliesWorkshopMigration() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '14' and success = true",
                Integer.class);

        assertThat(appliedMigrations).isEqualTo(1);
        Integer attendanceColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and table_name = 'registration' and column_name in "
                        + "('attendance_status', 'attendance_marked_at', 'attendance_marked_by')",
                Integer.class);
        assertThat(attendanceColumns).isEqualTo(3);
        Integer idempotencyColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and table_name = 'registration' and column_name = 'idempotency_key' and is_nullable = 'NO'",
                Integer.class);
        assertThat(idempotencyColumns).isEqualTo(1);
        Integer mobileMigration = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '16' and success = true",
                Integer.class);
        assertThat(mobileMigration).isEqualTo(1);
        Integer notificationUpdateColumn = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and table_name = 'notification' and column_name = 'updated_at' and is_nullable = 'NO'",
                Integer.class);
        assertThat(notificationUpdateColumn).isEqualTo(1);
        Integer operationMigration = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '18' and success = true",
                Integer.class);
        assertThat(operationMigration).isEqualTo(1);
        Integer operationColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and column_name = 'client_operation_id' and table_name in "
                        + "('post_comment', 'chat_message', 'evaluation')",
                Integer.class);
        assertThat(operationColumns).isEqualTo(3);
        Integer taxonomyMigration = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '19' and success = true",
                Integer.class);
        assertThat(taxonomyMigration).isEqualTo(1);
        Integer activeThemes = jdbcTemplate.queryForObject(
                "select count(*) from workshop.theme where active = true",
                Integer.class);
        Integer activeCategories = jdbcTemplate.queryForObject(
                "select count(*) from workshop.category where active = true",
                Integer.class);
        assertThat(activeThemes).isGreaterThanOrEqualTo(6);
        assertThat(activeCategories).isGreaterThanOrEqualTo(4);
        Integer demoMigration = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '20' and success = true",
                Integer.class);
        assertThat(demoMigration).isEqualTo(1);
        Integer demoWorkshops = jdbcTemplate.queryForObject(
                "select count(*) from workshop.workshop where id::text like '40000000-%'",
                Integer.class);
        assertThat(demoWorkshops).isEqualTo(6);
        String demoPasswordHash = jdbcTemplate.queryForObject(
                "select password_hash from workshop.app_user where username = 'demo.ana'",
                String.class);
        assertThat(passwordEncoder.matches("Workshop@2026!", demoPasswordHash)).isTrue();
    }

    @Test
    @Transactional
    void metricsAndAuditQueriesRunAgainstPostgreSql() {
        Integer migration = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '15' and success = true",
                Integer.class);
        assertThat(migration).isEqualTo(1);

        var page = PageRequest.of(0, 10);
        UserEntity user = users.save(UserEntity.create("Admin", "audit-admin", "audit@example.com",
                null, null, null, "hash", Role.ADMIN));
        Theme theme = themes.save(Theme.create("Audit theme", null));
        Category category = categories.save(Category.create("Audit category", null));
        LocalDate date = LocalDate.now().plusDays(7);
        WorkshopData data = new WorkshopData("Metrics workshop", "Description", null, date, date,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON,
                BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60),
                10, PaymentMethod.FREE, false, null);
        Workshop workshop = Workshop.create(data, theme, category, user);
        workshop.publish();
        workshops.save(workshop);
        Registration registration = Registration.create(user, workshop, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT);
        registration.markAttendance(AttendanceStatus.ATTENDED, user);
        registrations.save(registration);
        Post post = Post.create("Metrics post", "Content", null, workshop, category, false, user);
        post.publish();
        posts.save(post);
        likes.save(new PostLike(post, user));
        comments.save(PostComment.create(post, user, "Comment"));

        assertThat(metrics.workshops(WorkshopStatus.PUBLISHED, null, null, page).getContent())
                .filteredOn(result -> result.id().equals(workshop.getId()))
                .singleElement().satisfies(result -> {
                    assertThat(result.registrations()).isEqualTo(1);
                    assertThat(result.confirmed()).isEqualTo(1);
                    assertThat(result.attended()).isEqualTo(1);
                });
        assertThat(metrics.posts(PostStatus.PUBLISHED, null, null, page).getContent())
                .filteredOn(result -> result.id().equals(post.getId()))
                .singleElement().satisfies(result -> {
                    assertThat(result.likes()).isEqualTo(1);
                    assertThat(result.comments()).isEqualTo(1);
                });
        assertThat(workshopService.list(user.getId(), false, null, null, null,
                Instant.now().minusSeconds(60), page).getContent())
                .extracting(result -> result.id()).contains(workshop.getId());
        assertThat(workshopService.list(user.getId(), false, null, null, null,
                Instant.now().plusSeconds(60), page).getContent()).isEmpty();
        assertThat(postService.feed(user.getId(), Instant.now().minusSeconds(60), page).getContent())
                .extracting(result -> result.id()).contains(post.getId());
        assertThat(postService.feed(user.getId(), Instant.now().plusSeconds(60), page).getContent()).isEmpty();
        notificationService.notify(user.getId(), NotificationType.MANUAL, "Sync", "Updated", Map.of());
        assertThat(notificationService.list(user.getId(), Instant.now().minusSeconds(60), page).getContent())
                .singleElement().satisfies(result -> assertThat(result.updatedAt()).isNotNull());
        assertThat(notificationService.list(user.getId(), Instant.now().plusSeconds(60), page).getContent())
                .isEmpty();

        UUID entityId = UUID.randomUUID();
        audit.record(null, "PUBLISH", "WORKSHOP", entityId, "SCHEDULED", "PUBLISHED");
        assertThat(audit.search(null, "PUBLISH", "WORKSHOP", entityId, null, null, page).getContent())
                .singleElement().satisfies(result -> {
                    assertThat(result.previousValue()).isEqualTo("SCHEDULED");
                    assertThat(result.newValue()).isEqualTo("PUBLISHED");
                });
    }
}
