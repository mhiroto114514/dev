package com.example.advisor.judgement;

import com.example.advisor.school.Course;
import com.example.advisor.school.School;
import com.example.advisor.school.SchoolRepository;
import com.example.advisor.school.ScoreType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class JudgementService {

    private final SchoolRepository schoolRepository;
    public JudgementService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public JudgementResponse judge(JudgementRequest request) {
        JudgementResponse response = evaluate(request, false);
        validateStoredDeviationRange(request);
        return response;
    }

    public JudgementResponse preview(JudgementRequest request) {
        return evaluate(request, true);
    }

    private JudgementResponse evaluate(JudgementRequest request, boolean allowEmptyChoices) {
        List<Integer> ids = SchoolChoices.ids(request.desiredCourseCodes());
        if (!allowEmptyChoices && ids.stream().allMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("志望校を1校以上選択してください。");
        }
        return new JudgementResponse(ids.stream()
                .map(id -> id == null ? null : buildSchoolResult("course-" + id, request))
                .toList());
    }

    private SchoolJudgementResult buildSchoolResult(String courseCode, JudgementRequest request) {
        Course desiredCourse = schoolRepository.findCourseByCode(courseCode)
            .orElseThrow(() -> new IllegalArgumentException("志望コースが見つかりません。"));
        School desiredSchool = schoolRepository.findSchoolByCode(desiredCourse.schoolCode())
            .orElseThrow();

        double studentDeviation = resolveStudentDeviation(desiredCourse.scoreType(), request);

        double difference = studentDeviation - desiredCourse.deviationValue();

        return new SchoolJudgementResult(
            desiredSchool.name(),
            desiredCourse.department(),
            desiredCourse.courseName(),
            "",
            desiredSchool.schoolCategory(),
            toJudgement(difference),
            desiredCourse.scoreType(),
            studentDeviation,
            desiredCourse.deviationValue(),
            difference
        );
    }

    // The import endpoint previously rejected numeric overflow when INSERT ran.
    private void validateStoredDeviationRange(JudgementRequest request) {
        Double[] values = {request.japaneseDeviation(), request.mathDeviation(), request.englishDeviation(),
                request.scienceDeviation(), request.socialstudiesDeviation(), request.threeSubjectDeviation(),
                request.fiveSubjectDeviation(), request.saitamaDeviationThree(), request.saitamaDeviationFive()};
        for (Double value : values) {
            if (value != null && (!Double.isFinite(value) || BigDecimal.valueOf(value)
                    .setScale(1, RoundingMode.HALF_UP).abs().compareTo(new BigDecimal("999.9")) > 0)) {
                throw new IllegalArgumentException("偏差値が扱える範囲（-999.9〜999.9）を超えています。");
            }
        }
    }

    private double resolveStudentDeviation(ScoreType scoreType, JudgementRequest request) {
        Double threeSubjectAverage = average(
                request.japaneseDeviation(),
                request.mathDeviation(),
                request.englishDeviation()
        );
        Double fiveSubjectAverage = average(
                request.japaneseDeviation(),
                request.mathDeviation(),
                request.englishDeviation(),
                request.scienceDeviation(),
                request.socialstudiesDeviation()
        );

        Double selected;
        if (scoreType == ScoreType.THREE_SUBJECT) {
            selected = firstNonNull(
                    request.saitamaDeviationThree(),
                    request.threeSubjectDeviation(),
                    threeSubjectAverage,
                    request.saitamaDeviationFive(),
                    request.fiveSubjectDeviation(),
                    fiveSubjectAverage
            );
        } else {
            selected = firstNonNull(
                    request.saitamaDeviationFive(),
                    request.fiveSubjectDeviation(),
                    fiveSubjectAverage,
                    request.saitamaDeviationThree(),
                    request.threeSubjectDeviation(),
                    threeSubjectAverage
            );
        }

        if (selected == null) {
            throw new IllegalArgumentException("No deviation score is available for judgement.");
        }
        return selected;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double average(Double... values) {
        double sum = 0;
        int count = 0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return sum / count;
    }

    private String toJudgement(double difference) {
        if (difference >= 3) {
            return "A";
        }
        if (difference >= -2) {
            return "B";
        }
        if (difference >= -5) {
            return "C";
        }
        return "D";
    }
}
