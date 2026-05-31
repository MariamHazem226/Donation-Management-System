package com.givinghands.givinghands.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only schema validation. Never modifies the database.
 */
@Component
public class SchemaHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(SchemaHealthChecker.class);

    private static final List<String> CORE_TABLES = List.of("users", "campaigns");

    private static final Map<String, List<String>> REQUIRED_COLUMNS = Map.of(
            "users", List.of("id", "name", "email", "password", "role"),
            "campaigns", List.of(
                    "id", "title", "description", "category", "goal_amount", "current_amount",
                    "deadline", "status", "organization_id", "image_path", "allow_volunteers", "creator_id"
            ),
            "donations", List.of("id", "user_id", "campaign_id", "amount", "date", "status"),
            "volunteers", List.of(
                    "id", "user_id", "campaign_id", "status", "applied_date",
                    "why_join", "skills", "availability", "experience"
            )
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaHealthChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthReport check() {
        List<String> missingTables = new ArrayList<>();
        List<String> missingColumns = new ArrayList<>();

        for (String table : CORE_TABLES) {
            if (!tableExists(table)) {
                missingTables.add(table);
            }
        }

        for (Map.Entry<String, List<String>> entry : REQUIRED_COLUMNS.entrySet()) {
            String table = entry.getKey();
            if (missingTables.contains(table)) {
                continue;
            }
            if (!tableExists(table)) {
                missingTables.add(table);
                continue;
            }
            for (String column : entry.getValue()) {
                if (!columnExists(table, column)) {
                    missingColumns.add(table + "." + column);
                }
            }
        }

        boolean corePresent = missingTables.stream().noneMatch(CORE_TABLES::contains);
        boolean readyForSeeding = missingTables.isEmpty() && missingColumns.isEmpty();

        return new HealthReport(readyForSeeding, corePresent, missingTables, missingColumns);
    }

    public void logReport(HealthReport report) {
        if (report.readyForSeeding()) {
            log.info("[schema] VALID — all required tables and columns present (seeding allowed)");
            return;
        }

        log.error("[schema] INVALID — seeding is blocked until the database is repaired");
        if (!report.missingTables().isEmpty()) {
            log.error("[schema] Missing tables: {}", report.missingTables());
        }
        if (!report.missingColumns().isEmpty()) {
            log.error("[schema] Missing columns: {}", report.missingColumns());
        }
        log.error("[schema] Run backend/database/fix-schema.sql against givinghands_db, then restart");
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = ?
                """,
                Integer.class,
                table
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """,
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }

    public record HealthReport(
            boolean readyForSeeding,
            boolean coreTablesPresent,
            List<String> missingTables,
            List<String> missingColumns
    ) {
        /** @deprecated use {@link #readyForSeeding()} */
        public boolean healthy() {
            return readyForSeeding;
        }

        public List<String> issues() {
            List<String> all = new ArrayList<>();
            missingTables.forEach(t -> all.add("Missing table: " + t));
            missingColumns.forEach(c -> all.add("Missing column: " + c));
            return all;
        }
    }
}
