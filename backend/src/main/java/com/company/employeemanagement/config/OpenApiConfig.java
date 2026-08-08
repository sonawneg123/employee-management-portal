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
                        .description("Enterprise-grade REST API for managing employees, departments, "
                                + "attendance, leave requests, and performance reviews.")
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
                                        .description("Provide the JWT access token obtained from "
                                                + "/api/auth/login")));
    }
}
