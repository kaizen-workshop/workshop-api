package br.com.weg.workshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenApiCustomizer authenticationContract() {
        Set<String> publicPaths = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh",
                "/api/v1/auth/logout", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password");
        return api -> api.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            operation.setSecurity(publicPaths.contains(path) ? List.of()
                    : List.of(new SecurityRequirement().addList("bearerAuth")));
            if (!publicPaths.contains(path)) {
                operation.getResponses().addApiResponse("401", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Authentication is required or the access token is expired/revoked."));
                operation.getResponses().addApiResponse("403", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("The role, ownership or account state does not allow this operation."));
            }
        }));
    }

    @Bean
    OpenAPI workshopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workshop API")
                        .version("v1")
                        .description("API for ARWEG workshop management."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
