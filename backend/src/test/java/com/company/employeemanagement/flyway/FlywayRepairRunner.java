package com.company.employeemanagement.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standalone Flyway repair + migrate runner.
 *
 * Reads DB credentials from the project-root .env file so no credentials
 * are hard-coded. Run via:
 *
 *   mvnw exec:java -Dexec.mainClass=com.company.employeemanagement.flyway.FlywayRepairRunner
 */
public class FlywayRepairRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Flyway Repair Runner ===\n");

        // ── 1. Locate and load .env ───────────────────────────────────────────
        Map<String, String> env = loadDotEnv();

        // Build JDBC URL using the same defaults as application.properties
        String dbHost = env.getOrDefault("DB_HOST", "localhost");
        String dbPort = env.getOrDefault("DB_PORT", "3306");
        String dbName = env.getOrDefault("DB_NAME", "emp_portal");
        String dbUser = env.getOrDefault("DB_USER", "root");
        String dbPass = env.getOrDefault("DB_PASSWORD", "root");

        // Also try MYSQL_ROOT_PASSWORD as a fallback for root login
        if ("root".equals(dbUser) && "root".equals(dbPass)) {
            String rootPwd = env.get("MYSQL_ROOT_PASSWORD");
            if (rootPwd != null && !rootPwd.isBlank()) {
                dbPass = rootPwd;
            }
        }

        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            dbHost, dbPort, dbName);

        System.out.printf("Connecting: url=%s  user=%s%n%n", jdbcUrl, dbUser);

        // ── 2. Verify direct JDBC connection ──────────────────────────────────
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             Statement  stmt = conn.createStatement()) {

            System.out.println("=== flyway_schema_history (BEFORE repair) ===");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT installed_rank, version, description, type, success " +
                    "FROM flyway_schema_history ORDER BY installed_rank")) {
                while (rs.next()) {
                    System.out.printf("  rank=%-3d  v=%-5s  %-40s  type=%-4s  success=%d%n",
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getInt(5));
                }
            }

            System.out.println("\n=== employees columns ===");
            boolean hasFirst = false, hasLast = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employees' " +
                    "ORDER BY ORDINAL_POSITION")) {
                while (rs.next()) {
                    String col = rs.getString(1);
                    System.out.println("  " + col);
                    if ("first_name".equals(col)) hasFirst = true;
                    if ("last_name".equals(col))  hasLast  = true;
                }
            }
            System.out.printf("%n  first_name present BEFORE: %s%n", hasFirst);
            System.out.printf("  last_name  present BEFORE: %s%n%n", hasLast);
        }

        // ── 3. Build Flyway and run repair ────────────────────────────────────
        Flyway flyway = Flyway.configure()
            .dataSource(jdbcUrl, dbUser, dbPass)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();

        System.out.println("=== flyway.repair() ===");
        flyway.repair();
        System.out.println("  repair() done.\n");

        // ── 4. Migrate ────────────────────────────────────────────────────────
        System.out.println("=== flyway.migrate() ===");
        var result = flyway.migrate();
        System.out.printf("  migrationsExecuted=%d%n%n", result.migrationsExecuted);

        // ── 5. Verify final state via direct JDBC ─────────────────────────────
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             Statement  stmt = conn.createStatement()) {

            System.out.println("=== flyway_schema_history (AFTER repair+migrate) ===");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT installed_rank, version, description, type, success " +
                    "FROM flyway_schema_history ORDER BY installed_rank")) {
                while (rs.next()) {
                    System.out.printf("  rank=%-3d  v=%-5s  %-40s  type=%-4s  success=%d%n",
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getInt(5));
                }
            }

            System.out.println("\n=== employees columns (AFTER) ===");
            boolean firstAfter = false, lastAfter = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employees'")) {
                while (rs.next()) {
                    String col = rs.getString(1);
                    if ("first_name".equals(col)) firstAfter = true;
                    if ("last_name".equals(col))  lastAfter  = true;
                }
            }
            System.out.printf("  first_name present AFTER: %s%n", firstAfter);
            System.out.printf("  last_name  present AFTER: %s%n%n", lastAfter);

            // password_reset_tokens
            int prt = 0;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'password_reset_tokens'")) {
                if (rs.next()) prt = rs.getInt(1);
            }
            System.out.printf("  password_reset_tokens table present: %s%n%n", prt > 0);

            // Sample employee data
            System.out.println("=== Employee name sample ===");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT id, first_name, last_name FROM employees LIMIT 5")) {
                while (rs.next()) {
                    System.out.printf("  id=%.8s  first=%-15s  last=%s%n",
                        rs.getString(1), rs.getString(2), rs.getString(3));
                }
            }

            // ── 6. Assertions ─────────────────────────────────────────────────
            System.out.println("\n=== Assertions ===");
            assertTrue(firstAfter, "employees.first_name must exist");
            assertTrue(lastAfter,  "employees.last_name must exist");
            assertTrue(prt > 0,    "password_reset_tokens table must exist");

            for (MigrationInfo info : flyway.info().applied()) {
                System.out.printf("  V%-5s  %-40s  %s%n",
                    info.getVersion(), info.getDescription(), info.getState());
                if (info.getState() != MigrationState.SUCCESS &&
                    info.getState() != MigrationState.FUTURE_SUCCESS) {
                    throw new AssertionError(
                        "Migration V" + info.getVersion() + " state=" + info.getState());
                }
            }
        }

        System.out.println("\n=== ALL CHECKS PASSED — Flyway schema is healthy ===");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("FAILED: " + msg);
        System.out.println("  OK: " + msg);
    }

    /**
     * Parses KEY=VALUE lines from the project-root .env file.
     * Strips surrounding quotes and ignores comment lines.
     */
    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new LinkedHashMap<>();

        // Search from cwd up to three levels for .env
        Path[] candidates = {
            Paths.get(".env"),
            Paths.get("../.env"),
            Paths.get("../../.env"),
        };

        Path envFile = null;
        for (Path p : candidates) {
            if (Files.exists(p)) { envFile = p; break; }
        }

        if (envFile == null) {
            System.out.println("  [warn] .env not found — using application.properties defaults");
            return map;
        }

        System.out.println("  Loading credentials from: " + envFile.toAbsolutePath());
        try (BufferedReader br = new BufferedReader(new FileReader(envFile.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 1) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                // Strip optional surrounding quotes
                if (val.length() >= 2 &&
                    ((val.startsWith("\"") && val.endsWith("\"")) ||
                     (val.startsWith("'")  && val.endsWith("'")))) {
                    val = val.substring(1, val.length() - 1);
                }
                map.put(key, val);
            }
        } catch (Exception e) {
            System.out.println("  [warn] Could not read .env: " + e.getMessage());
        }
        return map;
    }
}
