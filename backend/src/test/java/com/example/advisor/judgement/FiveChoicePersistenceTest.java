package com.example.advisor.judgement;

import com.example.advisor.db.DeviationSchemaMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FiveChoicePersistenceTest {
    @Test void migrationPreservesRowsAndSavingRetainsAllFiveSlots() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        String schema = Files.readString(Path.of("src/main/resources/schema.sql")).split("DO \\$\\$")[0];
        // Normalize checkout line endings before constructing the three-choice schema.
        String oldSchema = schema.replace("\r\n", "\n").replace(",\n    fourth_choice INTEGER REFERENCES school(id),\n    fifth_choice INTEGER REFERENCES school(id)", "");
        for (String sql : oldSchema.split(";")) if (!sql.isBlank()) jdbc.execute(sql);
        for (int i = 1; i <= 5; i++) jdbc.update("INSERT INTO school(id,name,deviation) VALUES (?,?,?)", i, "School " + i, 50);
        jdbc.update("INSERT INTO student(student_id,name) VALUES (100,'Existing')");
        jdbc.update("INSERT INTO result(student_id,times,first_choice) VALUES (1,1,1)");
        var service = new JudgementPersistenceService(jdbc, mock(DeviationSchemaMigrationService.class));
        var request = new JudgementRequest(101, "New", 2, 50, 60, 70, null, null,
                50.1, 51.2, 52.3, null, null, 51.2, null, 53.4, null,
                Arrays.asList("course-1", null, "course-3", "course-4", "course-5"));
        assertThrows(IllegalArgumentException.class, () -> service.save(request));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM student", Integer.class));
        var migration = new ResourceDatabasePopulator(new FileSystemResource("db/migrations/001_five_choices.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);
        assertNull(jdbc.queryForMap("SELECT * FROM result WHERE times=1").get("fourth_choice"));
        service.save(request);
        var saved = jdbc.queryForMap("SELECT * FROM result WHERE times=2");
        assertEquals(1, saved.get("first_choice"));
        assertNull(saved.get("second_choice"));
        assertEquals(3, saved.get("third_choice"));
        assertEquals(4, saved.get("fourth_choice"));
        assertEquals(5, saved.get("fifth_choice"));
        assertNull(saved.get("science"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM result", Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE result SET fifth_choice=999 WHERE times=2"));
    }
}
