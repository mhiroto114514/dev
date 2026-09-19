package com.example.advisor.judgement;

import com.example.advisor.school.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsvPreviewTest {
    private SchoolRepository schools;
    private JudgementPersistenceService persistence;
    private CsvImportService csv;
    private static final String HEADER = "name,student_id,times,japanese,math,english,deviation_japanese,deviation_math,deviation_english,deviation_three,first_choice,second_choice,third_choice\n";

    @BeforeEach void setup() {
        schools = mock(SchoolRepository.class);
        persistence = mock(JudgementPersistenceService.class);
        Course course = new Course("course-1", "school-1", "", "School", ScoreType.FIVE_SUBJECT, 50);
        School school = new School("school-1", "School", SchoolCategory.PUBLIC);
        when(schools.readCourseByCode("course-1")).thenReturn(Optional.of(course));
        when(schools.readSchoolByCode("school-1")).thenReturn(Optional.of(school));
        when(schools.findCourseByCode("course-1")).thenReturn(Optional.of(course));
        when(schools.findSchoolByCode("school-1")).thenReturn(Optional.of(school));
        csv = new CsvImportService(new JudgementService(schools, persistence));
    }

    private MockMultipartFile file(String name, String rows) {
        return new MockMultipartFile("files", name, "text/csv", (HEADER + rows).getBytes(StandardCharsets.UTF_8));
    }

    private void fiveSchools() {
        for (int i = 2; i <= 5; i++) {
            var course = new Course("course-" + i, "school-" + i, "", "School " + i, ScoreType.FIVE_SUBJECT, 50);
            var school = new School("school-" + i, "School " + i, SchoolCategory.PRIVATE);
            when(schools.readCourseByCode("course-" + i)).thenReturn(Optional.of(course));
            when(schools.readSchoolByCode("school-" + i)).thenReturn(Optional.of(school));
            when(schools.findCourseByCode("course-" + i)).thenReturn(Optional.of(course));
            when(schools.findSchoolByCode("school-" + i)).thenReturn(Optional.of(school));
        }
    }

    private MockMultipartFile fiveFile(String choices) {
        return new MockMultipartFile("files", "five.csv", "text/csv", (HEADER.trim() + ",fourth_choice,fifth_choice\n"
                + "Student,1,1,50,50,50,50.1,50,50,50," + choices + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test void fiveChoicesWorkInBothModesAndOldCsvPadsToFive() {
        fiveSchools();
        var input = fiveFile("1,2,3,4,5");
        var preview = csv.preview(new MultipartFile[]{input}).ledgers().get(0);
        assertEquals(5, preview.results().size());
        assertEquals("School 5", preview.results().get(4).schoolName());
        assertEquals(preview, csv.importAndJudge(input).ledgers().get(0));
        var old = csv.preview(new MultipartFile[]{file("old.csv", "Student,1,1,50,50,50,50,50,50,50,1,,\n")}).ledgers().get(0);
        assertEquals(5, old.results().size());
        assertNull(old.results().get(4));
    }

    @Test void holesAreRetainedForPersistenceAndDuplicatesFailBeforeSaving() {
        fiveSchools();
        var imported = csv.importAndJudge(fiveFile("1,,3,,5")).ledgers().get(0);
        assertNull(imported.results().get(1));
        assertEquals("School 5", imported.results().get(4).schoolName());
        verify(persistence).save(argThat(r -> SchoolChoices.ids(r.desiredCourseCodes())
                .equals(java.util.Arrays.asList(1, null, 3, null, 5))));
        clearInvocations(persistence);
        var ex = assertThrows(IllegalArgumentException.class, () -> csv.importAndJudge(fiveFile("1,2,3,4,1")));
        assertTrue(ex.getMessage().contains("five.csv / 2行目"));
        assertTrue(ex.getMessage().contains("重複"));
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{fiveFile("1,2,3,4,1")}));
        verifyNoInteractions(persistence);
    }

    @Test void choiceValidationRejectsOversizeAndCanonicalDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> SchoolChoices.ids(java.util.List.of("course-1", "course-2", "course-3", "course-4", "course-5", "course-6")));
        assertThrows(IllegalArgumentException.class, () -> SchoolChoices.ids(java.util.List.of("course-1", "course-01")));
        assertThrows(IllegalArgumentException.class, () -> SchoolChoices.ids(java.util.List.of("course-0")));
    }

    @Test void groupsSortsDeduplicatesWithoutSavingAndPreservesSlots() {
        String second = "Student,2,2,0,50,50,50.1,50,50,50,,1,\n";
        var result = csv.preview(new MultipartFile[]{file("second.csv", second), file("first.csv",
                "Student,2,1,50,50,50,50,50,50,50,1,,\nOther,1,1,50,50,50,50,50,50,50,1,,\n"), file("copy.csv", second)});
        assertEquals(3, result.ledgers().size());
        assertEquals(1, result.ledgers().get(0).studentCode());
        assertEquals(1, result.ledgers().get(1).times());
        var row = result.ledgers().get(2);
        assertEquals(0, row.japaneseScore());
        assertEquals(50.1, row.japaneseDeviation());
        assertNull(row.scienceScore());
        assertNull(row.results().get(0));
        assertNotNull(row.results().get(1));
        assertNull(row.results().get(2));
        verifyNoInteractions(persistence);
        verify(schools, never()).findCourseByCode(anyString());
        verify(schools, never()).findSchoolByCode(anyString());
    }

    @Test void conflictsIdentifyBothSources() {
        var ex = assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{
                file("a.csv", "Student,1,1,50,50,50,50,50,50,50,1,,\n"),
                file("b.csv", "Student,1,1,51,50,50,50,50,50,50,1,,\n")}));
        assertTrue(ex.getMessage().contains("a.csv / 2"));
        assertTrue(ex.getMessage().contains("b.csv / 2"));
        verifyNoInteractions(persistence);
    }

    @Test void rejectsInvalidRoundNameAndSchool() {
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{file("range.csv", "Student,1,8,50,50,50,50,50,50,50,1,,\n")}));
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{file("name.csv", "Student,1,1,50,50,50,50,50,50,50,1,,\nOther,1,2,50,50,50,50,50,50,50,1,,\n")}));
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{file("school.csv", "Student,1,1,50,50,50,50,50,50,50,99,,\n")}));
        verifyNoInteractions(persistence);
    }

    @Test void emptyFilesFailAndNoChoicesAreAllowedForHistory() {
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[0]));
        assertThrows(IllegalArgumentException.class, () -> csv.preview(new MultipartFile[]{file("empty.csv", "")}));
        var row = csv.preview(new MultipartFile[]{file("none.csv", "Student,1,1,50,50,50,50,50,50,50,,,\n")}).ledgers().get(0);
        assertTrue(row.results().stream().allMatch(java.util.Objects::isNull));
        verifyNoInteractions(persistence);
    }

    @Test void originalImportStillSavesAndMatchesPreview() {
        var input = file("a.csv", "Student,1,1,50,50,50,50,50,50,50,1,,\n");
        var preview = csv.preview(new MultipartFile[]{input}).ledgers().get(0);
        var imported = csv.importAndJudge(input).ledgers().get(0);
        assertEquals(preview.results().get(0), imported.results().get(0));
        verify(persistence).save(any());
    }
}
