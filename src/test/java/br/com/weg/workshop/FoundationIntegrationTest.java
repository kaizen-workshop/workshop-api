package br.com.weg.workshop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.weg.workshop.shared.error.ConflictException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
