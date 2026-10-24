package com.example.advisor.judgement;

import com.example.advisor.db.DeviationSchemaMigrationService;
import com.example.advisor.school.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.*;
import java.util.*;

class LegacyDatabaseBaselineTest {
    @Test void captureIsolatedPostgresBaseline() throws Exception {
        String url = System.getProperty("baseline.jdbc.url");
        if (!"jdbc:postgresql://127.0.0.1:18432/postgres".equals(url)) {
            throw new IllegalStateException("Use the isolated temporary cluster on port 18432 only.");
        }
        try (var pool = new HikariDataSource()) {
        pool.setJdbcUrl(url);
        pool.setUsername("advisor_test");
        pool.setMaximumPoolSize(4);
        var jdbc = new JdbcTemplate(pool);
        String schema = Files.readString(Path.of("src/main/resources/schema.sql")).split("DO \\$\\$")[0];
        for (String statement : schema.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
        jdbc.execute("TRUNCATE TABLE result, student, school RESTART IDENTITY CASCADE");
        String source = Files.readString(Path.of("src/main/resources/data.sql"));
        // The authoritative file has a dangling comma; repair syntax only in this isolated test.
        String insert = source.substring(source.indexOf("INSERT INTO school")).replaceFirst(",\\s*;\\s*$", ";");
        jdbc.execute(insert);
        if (jdbc.queryForObject("SELECT count(*) FROM school", Integer.class) != 474) throw new AssertionError();
        var migration = new DeviationSchemaMigrationService(jdbc);
        var schools = new SchoolRepository(jdbc, migration);
        var service = new JudgementService(schools, new JudgementPersistenceService(jdbc, migration));
        var hashes = DatabaseRemovalCases.capture(new CsvImportService(service), schools);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("sourceRevision", "e12b5b0");
        baseline.put("database", "PostgreSQL 18, isolated cluster port 18432");
        baseline.put("source", "data.sql VALUES order defines IDs 1..474; trailing comma removed for execution only");
        baseline.put("cases", hashes);
        Path file = Path.of("src/test/resources/db-removal-baseline.json");
        Files.createDirectories(file.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), baseline);
        }
    }
}
