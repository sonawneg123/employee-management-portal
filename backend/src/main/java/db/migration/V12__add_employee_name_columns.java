package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Flyway Java migration V12 — adds {@code first_name} and {@code last_name}
 * columns to the {@code employees} table and back-fills them from the linked
 * {@code users} row.
 *
 * <p>A Java migration is used here instead of a plain SQL script because
 * MySQL 8.0 does not support {@code ALTER TABLE … ADD COLUMN IF NOT EXISTS}
 * (that is a MariaDB-only extension), and the JDBC protocol does not support
 * the MySQL CLI {@code DELIMITER} directive needed for stored procedures.
 * Java code gives us full control to inspect {@code DatabaseMetaData} before
 * issuing each {@code ALTER TABLE}, making this migration safely idempotent
 * regardless of the database state when it is applied.
 *
 * <p>This migration:
 * <ol>
 *   <li>Adds {@code employees.first_name VARCHAR(100) NULL} if absent.</li>
 *   <li>Adds {@code employees.last_name  VARCHAR(100) NULL} if absent.</li>
 *   <li>Back-fills both columns from the linked {@code users} row for every
 *       employee whose own columns are still {@code NULL}.</li>
 * </ol>
 */
public class V12__add_employee_name_columns extends BaseJavaMigration {

    @Override
    public void migrate(final Context context) throws Exception {
        Connection connection = context.getConnection();

        // Resolve the schema/catalog name for INFORMATION_SCHEMA queries.
        // getCatalog() returns the database name in MySQL.
        String catalog = connection.getCatalog();

        try (Statement stmt = connection.createStatement()) {

            // ── Add first_name if it does not already exist ──────────────────
            if (!columnExists(connection, catalog, "employees", "first_name")) {
                stmt.execute(
                    "ALTER TABLE employees "
                    + "ADD COLUMN first_name VARCHAR(100) NULL AFTER user_id"
                );
            }

            // ── Add last_name if it does not already exist ───────────────────
            if (!columnExists(connection, catalog, "employees", "last_name")) {
                stmt.execute(
                    "ALTER TABLE employees "
                    + "ADD COLUMN last_name VARCHAR(100) NULL AFTER first_name"
                );
            }

            // ── Back-fill from the linked users row ──────────────────────────
            // The WHERE guards make this UPDATE idempotent — rows already
            // populated are skipped on any subsequent run.
            stmt.execute(
                "UPDATE employees e "
                + "JOIN users u ON u.id = e.user_id "
                + "SET e.first_name = u.first_name, "
                + "    e.last_name  = u.last_name "
                + "WHERE e.user_id IS NOT NULL "
                + "  AND (e.first_name IS NULL OR e.last_name IS NULL)"
            );
        }
    }

    /**
     * Returns {@code true} when the specified column already exists in the
     * given table, using {@link DatabaseMetaData} so no SQL dialect is needed.
     *
     * @param connection the live JDBC connection
     * @param catalog    the MySQL database/catalog name
     * @param table      the table name (lower-case)
     * @param column     the column name (lower-case)
     * @return {@code true} if the column is present
     * @throws Exception if the metadata query fails
     */
    private boolean columnExists(final Connection connection,
                                  final String catalog,
                                  final String table,
                                  final String column) throws Exception {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
            return rs.next();
        }
    }
}
