package com.company.employeemanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 configuration for the Employee Management Portal.
 *
 * <p>Registers a {@link OpenAPI} bean that:
 * <ul>
 *   <li>Describes the API with title, version, and contact info.</li>
 *   <li>Defines a {@code BearerAuth} security scheme so that the
 *       Swagger UI can include the {@code Authorization} header on
 *       protected endpoint test calls.</li>
 *   <li>Applies {@code BearerAuth} as a global security requirement,
 *       meaning all operations require authentication unless individually
 *       overridden.</li>
 * </ul>
 *
 * <p>The Swagger UI is available at {@code /api/swagger-ui.html}.
 *
 * @author Employee Management Portal Team
 */
@Configuration
public class OpenApiConfig {

    /** Name of the JWT Bearer security scheme referenced in OpenAPI annotations. */
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * Builds and returns the customised {@link OpenAPI} descriptor.
     *
     * @return the fully configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI employeeManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management Portal API")
                        .version("1.0.0")
                        .description("""
                                Enterprise-grade REST API for managing employees, departments, \
                                leave requests, and performance reviews.

                                ## Authentication

                                All protected endpoints require a JWT Bearer token. \
                                Obtain one via `POST /api/auth/login` or `POST /api/auth/register`, \
                                then click **Authorize** (🔒) at the top of this page and paste the token.

                                ## Roles

                                | Role | Description |
                                |------|-------------|
                                | `ROLE_ADMIN` | Full access to all resources and delete operations |
                                | `ROLE_HR` | Create/update employees and departments; approve/reject leave |
                                | `ROLE_MANAGER` | Read employees and departments; approve/reject leave |
                                | `ROLE_EMPLOYEE` | Read own employee record; submit and manage own leave requests |

                                ## Error responses

                                All error responses use [RFC 7807 ProblemDetail](https://www.rfc-editor.org/rfc/rfc7807) \
                                with `application/problem+json` content type. \
                                The body includes `status`, `title`, `detail`, `timestamp`, and `path` fields.
                                """)
                        .contact(new Contact()
                                .name("Employee Management Portal Team")
                                .email("support@company.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://company.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer token obtained from POST /api/auth/login. "
                                                + "Paste the value of the `accessToken` field — "
                                                + "do NOT include the 'Bearer ' prefix here.")));
    }
}
