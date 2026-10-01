package br.com.weg.workshop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.service.UserAdministrationService;
import br.com.weg.workshop.auth.service.AuthenticationService;
import br.com.weg.workshop.user.service.ProfileService;
import br.com.weg.workshop.preference.service.CategoryService;
import br.com.weg.workshop.preference.service.PreferenceService;
import br.com.weg.workshop.preference.service.ThemeService;
import br.com.weg.workshop.workshop.service.WorkshopService;
import br.com.weg.workshop.workshop.dto.WorkshopResponse;
import br.com.weg.workshop.file.service.WorkshopMediaService;
import br.com.weg.workshop.registration.service.RegistrationService;
import br.com.weg.workshop.payment.service.PaymentService;
import br.com.weg.workshop.evaluation.service.EvaluationService;
import br.com.weg.workshop.evaluation.service.ParticipantWorkshopService;
import br.com.weg.workshop.post.service.PostService;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.group.service.GroupLifecycleService;
import br.com.weg.workshop.chat.service.ChatService;
import br.com.weg.workshop.notification.service.NotificationService;
import br.com.weg.workshop.administration.service.WorkshopAdministrationService;
import br.com.weg.workshop.audit.service.AuditService;
import br.com.weg.workshop.audit.service.MetricService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
@Import(FoundationIntegrationTest.FoundationTestController.class)
class FoundationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private MetricService metricService;

    @MockBean
    private UserAdministrationService userAdministrationService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private PreferenceService preferenceService;

    @MockBean
    private ThemeService themeService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private WorkshopService workshopService;

    @MockBean
    private WorkshopMediaService workshopMediaService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private EvaluationService evaluationService;

    @MockBean
    private ParticipantWorkshopService participantWorkshopService;

    @MockBean
    private PostService postService;

    @MockBean
    private GroupService groupService;

    @MockBean
    private ChatService chatService;

    @MockBean
    private GroupLifecycleService groupLifecycleService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private WorkshopAdministrationService workshopAdministrationService;


    @Test
    void healthEndpointIsPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocumentExposesJwtBearerScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Workshop API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void unauthenticatedApiRequestReturnsStandardUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/foundation-test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/foundation-test/protected"));
    }

    @Test
    void profileAndTaxonomyEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/themes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/users/me/themes").contentType("application/json").content("{\"themeIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void participantCannotManageTaxonomy() throws Exception {
        mockMvc.perform(post("/api/v1/admin/themes")
                        .with(user("participant").roles("PARTICIPANT"))
                        .contentType("application/json")
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void workshopCatalogueRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/workshops"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void groupsAndChatRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/groups/{id}/messages", java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificationsRequireAuthenticationAndManualCommunicationRequiresArweg() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/arweg/notifications")
                        .with(user("participant").roles("PARTICIPANT"))
                        .contentType("application/json")
                        .content("{\"userIds\":[\"" + java.util.UUID.randomUUID()
                                + "\"],\"title\":\"Notice\",\"message\":\"Message\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void workshopAdministrationRequiresArweg() throws Exception {
        mockMvc.perform(get("/api/v1/arweg/dashboard")
                        .with(user(java.util.UUID.randomUUID().toString()).roles("PARTICIPANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/arweg/workshops/{id}/participants", java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workshopDetailSupportsPrivateConditionalCaching() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();
        java.util.UUID workshopId = java.util.UUID.randomUUID();
        java.time.Instant updatedAt = java.time.Instant.parse("2026-10-01T10:00:00Z");
        WorkshopResponse response = mock(WorkshopResponse.class);
        when(response.id()).thenReturn(workshopId);
        when(response.updatedAt()).thenReturn(updatedAt);
        when(workshopService.get(userId, false, workshopId)).thenReturn(response);
        String etag = "\"" + workshopId + "-" + updatedAt + "\"";

        mockMvc.perform(get("/api/v1/workshops/{id}", workshopId)
                        .with(user(userId.toString()).roles("PARTICIPANT"))
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void metricsAndAuditRequireAdmin() throws Exception {
        for (String path : new String[] {"/api/v1/admin/metrics/workshops", "/api/v1/admin/metrics/posts",
                "/api/v1/admin/audit"}) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).with(user("arweg").roles("ARWEG")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void participantCannotCreateWorkshop() throws Exception {
        mockMvc.perform(patch("/api/v1/workshops/{id}/publish", java.util.UUID.randomUUID())
                        .with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void registrationRequiresAuthenticationAndOnlyArwegCanManageWorkshopRegistrations() throws Exception {
        mockMvc.perform(post("/api/v1/workshops/{id}/registrations", java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/workshops/{id}/registrations", java.util.UUID.randomUUID())
                        .with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/v1/workshops/{id}/registrations", java.util.UUID.randomUUID())
                        .with(user(java.util.UUID.randomUUID().toString()).roles("PARTICIPANT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void paymentCreationRequiresAuthenticationAndSimulationRequiresArweg() throws Exception {
        mockMvc.perform(post("/api/v1/registrations/{id}/payments", java.util.UUID.randomUUID())
                        .header("Idempotency-Key", java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/payments/{id}/simulate/paid", java.util.UUID.randomUUID())
                        .with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthorizedRoleReturnsStandardForbiddenError() throws Exception {
        mockMvc.perform(get("/api/v1/foundation-test/admin").with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validationErrorUsesStandardContract() throws Exception {
        mockMvc.perform(post("/api/v1/foundation-test/validation")
                        .with(user("participant").roles("PARTICIPANT"))
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void conflictUsesStandardContract() throws Exception {
        mockMvc.perform(get("/api/v1/foundation-test/conflict").with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void unknownAuthenticatedPathReturnsStandardNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/foundation-test/unknown").with(user("participant").roles("PARTICIPANT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @RestController
    @RequestMapping("/api/v1/foundation-test")
    static class FoundationTestController {

        @GetMapping("/protected")
        String protectedEndpoint() {
            return "ok";
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminEndpoint() {
            return "ok";
        }

        @PostMapping("/validation")
        String validate(@Valid @RequestBody FoundationRequest request) {
            return request.name();
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException("Conflict");
        }
    }

    record FoundationRequest(@NotBlank String name) {
    }
}
