package com.example.advisor.judgement;

import com.example.advisor.school.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseRemovalParityTest {
    @Test void matchesAllFrozenPostgresResponses() throws Exception {
        var schools = new SchoolRepository(new ClassPathResource("school-master.csv"));
        var csv = new CsvImportService(new JudgementService(schools));
        var baseline = new ObjectMapper().readTree(new ClassPathResource("db-removal-baseline.json").getInputStream());
        Map<String, String> actual = DatabaseRemovalCases.capture(csv, schools);
        assertEquals(baseline.get("cases").size(), actual.size());
        assertAll(actual.entrySet().stream().map(e -> () -> assertEquals(
                baseline.get("cases").get(e.getKey()).asText(), e.getValue(), e.getKey())));
    }
}
