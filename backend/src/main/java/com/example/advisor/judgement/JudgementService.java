package com.example.advisor.judgement;

import com.example.advisor.school.Course;
import com.example.advisor.school.School;
import com.example.advisor.school.SchoolRepository;
import com.example.advisor.school.ScoreType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JudgementService {

    private final SchoolRepository schoolRepository;
    private final JudgementPersistenceService judgementPersistenceService;

    public JudgementService(SchoolRepository schoolRepository, JudgementPersistenceService judgementPersistenceService) {
        this.schoolRepository = schoolRepository;
        this.judgementPersistenceService = judgementPersistenceService;
    }

    public JudgementResponse judge(JudgementRequest request) {
        JudgementResponse response = evaluate(request, false);
        judgementPersistenceService.save(request);
        return response;
    }

    public JudgementResponse preview(JudgementRequest request) {
        return evaluate(request, true);
    }

    private JudgementResponse evaluate(JudgementRequest request, boolean readOnly) {
        List<Integer> ids = SchoolChoices.ids(request.desiredCourseCodes());
        if (!readOnly && ids.stream().allMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("志望校を1校以上選択してください。");
        }
        return new JudgementResponse(ids.stream()
                .map(id -> id == null ? null : buildSchoolResult("course-" + id, request, readOnly))
                .toList());
    }

    private SchoolJudgementResult buildSchoolResult(String courseCode, JudgementRequest request, boolean readOnly) {
        Course desiredCourse = (readOnly ? schoolRepository.readCourseByCode(courseCode) : schoolRepository.findCourseByCode(courseCode))
            .orElseThrow(() -> new IllegalArgumentException("志望コースが見つかりません。"));
        School desiredSchool = (readOnly ? schoolRepository.readSchoolByCode(desiredCourse.schoolCode()) : schoolRepository.findSchoolByCode(desiredCourse.schoolCode()))
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
