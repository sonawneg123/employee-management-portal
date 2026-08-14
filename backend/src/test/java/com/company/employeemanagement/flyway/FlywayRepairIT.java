package com.company.employeemanagement.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway repair integration test.
 *
 * Disables Spring Boot's automatic Flyway migration so we can call
 * repair() and migrate() manually in the correct order.
 *
 * Run standalone:  mvnw test -Dtest=FlywayRepairIT
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Prevent Spring Boot auto-running Flyway on context startup —
        // we call repair() + migrate() ourselves below.
        "spring.flyway.enabled=false",
        // Prevent Hibernate DDL validation from running without Flyway tables
        "spring.jpa.hibernate.ddl-auto=none"
})
class FlywayRepairIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void repairAndMigrate() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // ── Build Flyway instance pointing at the live DB ─────────────────────
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                // Mirrors application.properties settings
                .baselineOnMigrate(true)
                .load();

        // ── 1. Show history BEFORE repair ─────────────────────────────────────
        System.out.println("\n=== flyway_schema_history  (BEFORE repair) ===");
        printHistory(jdbc);

        // ── 2. Show employees columns ─────────────────────────────────────────
        System.out.println("\n=== employees columns ===");
        boolean hadFirstName = printColumnsAndCheck(jdbc, "first_name");
        boolean hadLastName  = printColumnsAndCheck(jdbc, "last_name");
        System.out.printf("  first_name present BEFORE repair: %s%n", hadFirstName);
        System.out.printf("  last_name  present BEFORE repair: %s%n", hadLastName);

        // ── 3. Repair — removes the failed V12 history entry ──────────────────
        System.out.println("\n=== flyway.repair() ===");
        flyway.repair();
        System.out.println("  repair() done.");

        // ── 4. Migrate — V12 runs idempotently, V13 creates password_reset_tokens
        System.out.println("\n=== flyway.migrate() ===");
        var result = flyway.migrate();
        System.out.printf("  migrationsExecuted=%d%n", result.migrationsExecuted);

        // ── 5. Show history AFTER repair+migrate ──────────────────────────────
        System.out.println("\n=== flyway_schema_history  (AFTER repair+migrate) ===");
        printHistory(jdbc);

        // ── 6. Final column check ─────────────────────────────────────────────
        boolean firstNameNow = printColumnsAndCheck(jdbc, "first_name");
        boolean lastNameNow  = printColumnsAndCheck(jdbc, "last_name");

        // ── 7. Verify password_reset_tokens exists (V13) ─────────────────────
        Integer prtCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'password_reset_tokens'",
                Integer.class);
        System.out.printf("%n  password_reset_tokens table present: %s%n", prtCount > 0);

        // ── 8. Print all applied migrations + their state ─────────────────────
        System.out.println("\n=== Applied migration states ===");
        for (MigrationInfo info : flyway.info().applied()) {
            System.out.printf("  V%-5s  %-45s  state=%s%n",
                    info.getVersion(), info.getDescription(), info.getState());
        }

        // ── 9. Sample employee data ───────────────────────────────────────────
        System.out.println("\n=== Employee name sample (first 5 rows) ===");
        try {
            List<Map<String, Object>> emps = jdbc.queryForList(
                    "SELECT id, first_name, last_name FROM employees LIMIT 5");
            emps.forEach(e -> System.out.printf("  id=%.8s  first=%-15s  last=%s%n",
                    e.get("id"), e.get("first_name"), e.get("last_name")));
        } catch (Exception e) {
            System.out.println("  (could not query employees: " + e.getMessage() + ")");
        }

        // ── Assertions ────────────────────────────────────────────────────────
        assertThat(firstNameNow).as("employees.first_name must exist after V12").isTrue();
        assertThat(lastNameNow ).as("employees.last_name must exist after V12").isTrue();
        assertThat(prtCount    ).as("password_reset_tokens table must exist after V13").isEqualTo(1);

        for (MigrationInfo info : flyway.info().applied()) {
            assertThat(info.getState())
                    .as("Migration V%s must be SUCCESS", info.getVersion())
                    .isIn(MigrationState.SUCCESS, MigrationState.FUTURE_SUCCESS);
        }

        System.out.println("\n=== ALL ASSERTIONS PASSED ===");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void printHistory(JdbcTemplate jdbc) {
        try {
            jdbc.queryForList(
                    "SELECT installed_rank, version, description, type, success " +
                    "FROM flyway_schema_history ORDER BY installed_rank")
                .forEach(r -> System.out.printf(
                    "  rank=%-3s  v=%-5s  %-40s  type=%-4s  success=%s%n",
                    r.get("installed_rank"), r.get("version"),
                    r.get("description"), r.get("type"), r.get("success")));
        } catch (Exception e) {
            System.out.println("  (no history table yet: " + e.getMessage() + ")");
        }
    }

    /** Returns true if the named column exists in employees. */
    private boolean printColumnsAndCheck(JdbcTemplate jdbc, String colName) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employees' " +
                "AND COLUMN_NAME = ?",
                Integer.class, colName);
        return cnt != null && cnt > 0;
    }
}
