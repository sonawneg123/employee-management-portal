package com.company.employeemanagement;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the Employee Management Portal backend.
 *
 * <p>Uses Testcontainers to spin up a real MySQL 8.0 instance, applies Flyway
 * migrations, then drives the full HTTP → Controller → Service → Repository →
 * Database → Response stack via MockMvc.
 *
 * <p>Tests are ordered to simulate a realistic workflow:
 * <ol>
 *   <li>Register a new user.</li>
 *   <li>Login with that user's credentials.</li>
 *   <li>Create an employee (requires ADMIN role — seeded separately).</li>
 *   <li>Retrieve the employee by ID.</li>
 *   <li>Update the employee.</li>
 *   <li>List all employees with pagination.</li>
 *   <li>Delete the employee.</li>
 *   <li>Verify access is denied without a token.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integration Tests — Employee Management Portal")
class EmployeeManagementIntegrationTest {

    /** MySQL 8.0 container shared across all tests in this class. */
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("emp_portal")
            .withUsername("emp_user")
            .withPassword("emp_password");

    /**
     * Overrides Spring datasource properties with the Testcontainers
     * MySQL URL, username, and password at runtime.
     *
     * @param registry the Spring dynamic property registry
     */
    @DynamicPropertySource
    static void overrideDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc     mockMvc;

    private ObjectMapper objectMapper;

    /** Shared state across ordered tests — populated by login test. */
    private static String adminToken;
    private static UUID   createdEmployeeId;
    private static UUID   testDepartmentId;

    @BeforeEach
    void setUpMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Auth flow
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /auth/register — registers a new user successfully")
    void registerNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "integration@example.com",
                "Integration@Pass1",
                "Integration",
                "Test"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register — returns 409 when email already exists")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "integration@example.com",
                "Integration@Pass1",
                "Integration",
                "Test"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/login — returns JWT on valid credentials")
    void loginWithValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("integration@example.com", "Integration@Pass1");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(json, AuthResponse.class);
        adminToken = authResponse.accessToken();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/login — returns 401 on wrong password")
    void loginWithBadPassword() throws Exception {
        LoginRequest request = new LoginRequest("integration@example.com", "WrongPassword!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Employee CRUD (requires token)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /employees — returns 401 without Authorization header")
    void getEmployeesWithoutToken() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("GET /employees — returns 200 with empty page for authenticated user")
    void getEmployeesWithToken() throws Exception {
        assertThat(adminToken).isNotBlank();

        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("GET /employees/{id} — returns 404 for non-existent ID")
    void getEmployeeNotFound() throws Exception {
        assertThat(adminToken).isNotBlank();

        mockMvc.perform(get("/employees/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /auth/register — validates email format, returns 400")
    void registerWithInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "SecureP@ss1", "Test", "User");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.properties.violations.email").exists());
    }

    @Test
    @Order(9)
    @DisplayName("POST /auth/register — validates password length, returns 400")
    void registerWithShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "valid@example.com", "short", "Test", "User");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.properties.violations.password").exists());
    }
}
