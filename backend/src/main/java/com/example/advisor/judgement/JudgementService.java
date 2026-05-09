package com.example.advisor.judgement;

import com.example.advisor.school.RecommendationType;
import com.example.advisor.school.RecommendedSchool;
import com.example.advisor.school.ScoreType;
import com.example.advisor.school.Course;
import com.example.advisor.school.School;
import com.example.advisor.school.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class JudgementService {

    private final SchoolRepository schoolRepository;

    public JudgementService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public JudgementResponse judge(JudgementRequest request) {
        List<String> desiredCourseCodes = new ArrayList<>();
        desiredCourseCodes.addAll(request.publicDesiredCourseCodes());
        desiredCourseCodes.addAll(request.privateDesiredCourseCodes());

        if (desiredCourseCodes.isEmpty()) {
            throw new IllegalArgumentException("\u5fd7\u671b\u6821\u30921\u6821\u4ee5\u4e0a\u9078\u629e\u3057\u3066\u304f\u3060\u3055\u3044\u3002");
        }

        List<SchoolJudgementResult> results = desiredCourseCodes.stream()
                .distinct()
                .map(code -> buildSchoolResult(code, request))
                .toList();

        return new JudgementResponse(results);
    }

    private SchoolJudgementResult buildSchoolResult(String courseCode, JudgementRequest request) {
        Course desiredCourse = schoolRepository.findCourseByCode(courseCode)
                .orElseThrow(() -> new IllegalArgumentException("\u5fd7\u671b\u30b3\u30fc\u30b9\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093\u3002"));
        School desiredSchool = schoolRepository.findSchoolByCode(desiredCourse.schoolCode())
                .orElseThrow();

        int studentDeviation = desiredCourse.scoreType() == ScoreType.THREE_SUBJECT
                ? request.threeSubjectDeviation()
                : request.fiveSubjectDeviation();

        int difference = studentDeviation - desiredCourse.deviationValue();

        return new SchoolJudgementResult(
                desiredSchool.name(),
                desiredCourse.department(),
                desiredCourse.courseName(),
                desiredSchool.municipality().getLabel(),
                desiredSchool.schoolCategory(),
                toJudgement(difference),
                desiredCourse.scoreType(),
                studentDeviation,
                desiredCourse.deviationValue(),
                difference,
                buildRecommendations(desiredSchool, desiredCourse, studentDeviation)
        );
    }

    private String toJudgement(int difference) {
        if (difference >= 2) {
            return "A";
        }
        if (difference >= 0) {
            return "B";
        }
        if (difference >= -2) {
            return "C";
        }
        if (difference >= -4) {
            return "D";
        }
        return "E";
    }

    private List<RecommendedSchool> buildRecommendations(School desiredSchool, Course desiredCourse, int studentDeviation) {
        List<Course> candidates = schoolRepository.findAllCourses().stream()
                .filter(course -> !course.code().equals(desiredCourse.code()))
                .filter(course -> course.scoreType() == desiredCourse.scoreType())
                .filter(course -> schoolRepository.findSchoolByCode(course.schoolCode())
                        .map(School::schoolCategory)
                        .filter(category -> category == desiredSchool.schoolCategory())
                        .isPresent())
                .toList();

        List<RecommendedSchool> recommendations = new ArrayList<>();

        findChallengeSchool(candidates, studentDeviation).ifPresent(recommendations::add);
        findMatchSchool(candidates, studentDeviation).ifPresent(recommendations::add);
        findSafetySchool(candidates, studentDeviation).ifPresent(recommendations::add);

        return recommendations;
    }

    private java.util.Optional<RecommendedSchool> findChallengeSchool(List<Course> candidates, int studentDeviation) {
        return candidates.stream()
                .filter(course -> course.deviationValue()- studentDeviation >= 3)
                .min(Comparator.comparingInt(course -> course.deviationValue() - studentDeviation))
                .map(course -> toRecommendedSchool(course, RecommendationType.CHALLENGE));
    }

    private java.util.Optional<RecommendedSchool> findMatchSchool(List<Course> candidates, int studentDeviation) {
        return candidates.stream()
                .filter(course -> Math.abs(course.deviationValue() - studentDeviation) <= 2)
                .min(Comparator.comparingInt(course -> Math.abs(course.deviationValue() - studentDeviation)))
                .map(course -> toRecommendedSchool(course, RecommendationType.MATCH));
    }

    private java.util.Optional<RecommendedSchool> findSafetySchool(List<Course> candidates, int studentDeviation) {
        return candidates.stream()
                .filter(course -> studentDeviation - course.deviationValue() >= 3)
                .max(Comparator.comparingInt(Course::deviationValue))
                .map(course -> toRecommendedSchool(course, RecommendationType.SAFETY));
    }

    private RecommendedSchool toRecommendedSchool(Course course, RecommendationType recommendationType) {
        School school = schoolRepository.findSchoolByCode(course.schoolCode())
                .orElseThrow();

        return new RecommendedSchool(
                course.code(),
                school.name(),
                course.department(),
                course.courseName(),
                school.schoolCategory(),
                course.scoreType(),
                course.deviationValue(),
                school.municipality().getLabel(),
                recommendationType
        );
    }
}
