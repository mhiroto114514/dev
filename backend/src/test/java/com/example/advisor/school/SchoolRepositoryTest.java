package com.example.advisor.school;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SchoolRepositoryTest {
    private static final String HEADER = "id,name,deviation,school_category\n";
    private SchoolRepository load(String body) {
        return new SchoolRepository(new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void everyIdMatchesTheAuthoritativeSqlInOriginalOrder() throws Exception {
        String sql = new String(new ClassPathResource("legacy-data.sql").getContentAsByteArray(), StandardCharsets.UTF_8);
        var tuple = Pattern.compile("\\('((?:''|[^'])*)',\\s*([0-9]+(?:\\.[0-9]+)?),\\s*'([^']+)'\\)").matcher(sql);
        var schools = new SchoolRepository(new ClassPathResource("school-master.csv"));
        int id = 0;
        while (tuple.find()) {
            id++;
            var school = schools.findSchoolByCode("school-" + id).orElseThrow();
            var course = schools.findCourseByCode("course-" + id).orElseThrow();
            assertEquals(tuple.group(1).replace("''", "'"), school.name());
            assertEquals(SchoolCategory.valueOf(tuple.group(3)), school.schoolCategory());
            assertEquals(Double.parseDouble(tuple.group(2)), course.deviationValue());
            assertEquals(school.name(), course.courseName());
            assertEquals(ScoreType.FIVE_SUBJECT, course.scoreType());
        }
        assertEquals(474, id);
        long masterRows = new ClassPathResource("school-master.csv").getContentAsString(StandardCharsets.UTF_8)
                .lines().skip(1).filter(line -> !line.isBlank()).count();
        assertEquals(id, masterRows);
    }

    @Test void explicitIdsDoNotDependOnCsvOrderAndNamesArePreserved() {
        var schools = load("\uFEFF" + HEADER + "7,\"School, \"\"Special\"\"\",60.25,PRIVATE\n2,School B,60.3,NATIONAL\n9,School C,60.3,KOSEN\n");
        assertEquals("School, \"Special\"", schools.findSchoolByCode("school-7").orElseThrow().name());
        assertEquals(60.3, schools.findCourseByCode("course-7").orElseThrow().deviationValue());
        assertEquals("School B", schools.findSchoolByCode("school-2").orElseThrow().name());
        assertEquals(SchoolCategory.NATIONAL, schools.findSchoolByCode("school-2").orElseThrow().schoolCategory());
        assertEquals(SchoolCategory.KOSEN, schools.findSchoolByCode("school-9").orElseThrow().schoolCategory());
        assertTrue(schools.findSchoolByCode("school-123").isEmpty());
    }

    @Test void malformedOrMissingMasterFailsRatherThanLoadingPartialData() {
        for (String body : new String[]{"", HEADER, HEADER + "1,A,50,PUBLIC\n1,B,60,PRIVATE\n",
                HEADER + "0,A,50,PUBLIC\n", HEADER + "1,,50,PUBLIC\n", HEADER + "1,A,NaN,PUBLIC\n",
                HEADER + "1,A,1000,PUBLIC\n", HEADER + "1,A,50,UNKNOWN\n", HEADER + "1,\"Unclosed,50,PUBLIC\n",
                HEADER + "1,A,50,PUBLIC,extra\n"}) {
            assertThrows(IllegalStateException.class, () -> load(body), body);
        }
        assertThrows(IllegalStateException.class, () -> new SchoolRepository(new ClassPathResource("missing-school-master.csv")));
    }
}
