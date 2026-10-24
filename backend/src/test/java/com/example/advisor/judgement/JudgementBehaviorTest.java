package com.example.advisor.judgement;

import com.example.advisor.school.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JudgementBehaviorTest {
    private final JudgementService service = new JudgementService(new SchoolRepository(new ClassPathResource("school-master.csv")));
    private JudgementRequest request(Double five, Double saitamaFive) {
        return new JudgementRequest(1, "Student", 1, 0, 0, 0, null, null,
                50.0, 51.0, 52.0, null, null, 60.0, five, 80.0, saitamaFive, List.of("course-1"));
    }

    @Test void exactBoundariesAndLowScoresKeepTheExistingJudgementRules() {
        double[] deviations = {75, 74.9, 70, 69.9, 67, 66.9};
        String[] expected = {"A", "B", "B", "C", "C", "D"};
        for (int i = 0; i < deviations.length; i++) {
            assertEquals(expected[i], service.judge(request(40.0, deviations[i])).results().get(0).judgement());
        }
    }

    @Test void missingFiveSubjectValuesUseGeneralThenAverageAsBefore() {
        assertEquals(66.6, service.judge(request(66.6, null)).results().get(0).studentDeviationValue());
        assertEquals(51.0, service.judge(request(null, null)).results().get(0).studentDeviationValue());
    }

    @Test void importStillRejectsFormerDatabaseNumericOverflow() {
        assertThrows(IllegalArgumentException.class, () -> service.judge(request(1000.0, null)));
        assertEquals(999.94, service.judge(request(null, 999.94)).results().get(0).studentDeviationValue());
        assertThrows(IllegalArgumentException.class, () -> service.judge(request(null, 999.95)));
        // Preview never stored values and continues to have no storage-specific range limit.
        assertEquals(1000.0, service.preview(request(1000.0, null)).results().get(0).studentDeviationValue());
    }
}
