package com.company.employeemanagement;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for all integration tests that require a real MySQL
 * database managed by Testcontainers.
 *
 * <p>By extending this class, concrete test classes automatically:
 * <ul>
 *   <li>Boot the full Spring context in a random port.</li>
 *   <li>Share a single {@link MySQLContainer} instance across all
 *       inheriting test classes (static field).</li>
 *   <li>Have their datasource properties auto-configured via
 *       {@link DynamicPropertySource}.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * MySQL 8.0 container shared across all integration test classes that
     * extend this base. Testcontainers keeps a single container alive for
     * the entire test suite lifecycle, saving startup overhead.
     */
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("emp_portal_test")
            .withUsername("test_user")
            .withPassword("test_password");

    /**
     * Overrides Spring Boot datasource and Flyway properties with the
     * Testcontainers MySQL connection details at runtime.
     *
     * @param registry the dynamic property registry provided by Spring Test
     */
    @DynamicPropertySource
    static void overrideProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
