package com.example.advisor.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DeviationSchemaMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final Object migrationLock = new Object();
    private volatile boolean migrated;

    public DeviationSchemaMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureMigrated() {
        if (migrated) {
            return;
        }
        synchronized (migrationLock) {
            if (migrated) {
                return;
            }

            migrateIfNeeded("school", "deviation");
            migrateIfNeeded("result", "deviation_japanese");
            migrateIfNeeded("result", "deviation_math");
            migrateIfNeeded("result", "deviation_english");
            migrateIfNeeded("result", "deviation_science");
            migrateIfNeeded("result", "deviation_socialstudies");
            migrateIfNeeded("result", "deviation_socialscience");
            migrateIfNeeded("result", "deviation_three");
            migrateIfNeeded("result", "deviation_five");
            migrateIfNeeded("result", "saitama_deviation_three");
            migrateIfNeeded("result", "saitama_deviation_five");

            migrated = true;
        }
    }

    private void migrateIfNeeded(String tableName, String columnName) {
        List<String> dataTypes = jdbcTemplate.queryForList(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """,
                String.class,
                tableName,
                columnName
        );

        if (dataTypes.isEmpty()) {
            return;
        }

        String dataType = dataTypes.get(0) == null ? "" : dataTypes.get(0).toLowerCase(Locale.ROOT);
        if ("numeric".equals(dataType) || "decimal".equals(dataType)) {
            return;
        }

        String sql = "ALTER TABLE " + tableName
                + " ALTER COLUMN " + columnName
                + " TYPE NUMERIC(4,1) USING ROUND(" + columnName + "::numeric, 1)";
        jdbcTemplate.execute(sql);
    }
}
