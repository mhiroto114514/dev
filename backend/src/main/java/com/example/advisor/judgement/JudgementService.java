package com.example.advisor.judgement;

import com.example.advisor.school.Course;
import com.example.advisor.school.School;
import com.example.advisor.school.SchoolRepository;
import com.example.advisor.school.ScoreType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        List<String> desiredCourseCodes = new ArrayList<>(request.desiredCourseCodes());

        if (desiredCourseCodes.isEmpty()) {
            throw new IllegalArgumentException("志望校を1校以上選択してください。");
        }

        List<SchoolJudgementResult> results = desiredCourseCodes.stream()
            .filter(code -> code != null && !code.isBlank())
            .distinct()
            .map(code -> buildSchoolResult(code, request))
            .toList();

        if (results.isEmpty()) {
            throw new IllegalArgumentException("志望校を1校以上選択してください。");
        }

        Integer firstChoice = toSchoolId(desiredCourseCodes, 0);
        Integer secondChoice = toSchoolId(desiredCourseCodes, 1);
        Integer thirdChoice = toSchoolId(desiredCourseCodes, 2);
        judgementPersistenceService.save(request, firstChoice, secondChoice, thirdChoice);

        return new JudgementResponse(results);
    }

    private Integer toSchoolId(List<String> desiredCourseCodes, int index) {
        List<String> distinctCodes = desiredCourseCodes.stream()
            .filter(code -> code != null && !code.isBlank())
            .distinct()
            .toList();
        if (index >= distinctCodes.size()) {
            return null;
        }
        String code = distinctCodes.get(index);
        if (!code.startsWith("course-")) {
            throw new IllegalArgumentException("コースコードの形式が不正です。");
        }
        return Integer.parseInt(code.substring("course-".length()));
    }

    private SchoolJudgementResult buildSchoolResult(String courseCode, JudgementRequest request) {
        Course desiredCourse = schoolRepository.findCourseByCode(courseCode)
            .orElseThrow(() -> new IllegalArgumentException("志望コースが見つかりません。"));
        School desiredSchool = schoolRepository.findSchoolByCode(desiredCourse.schoolCode())
            .orElseThrow();

        int studentDeviation = resolveStudentDeviation(desiredCourse.scoreType(), request);

        int difference = studentDeviation - desiredCourse.deviationValue();

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

    private int resolveStudentDeviation(ScoreType scoreType, JudgementRequest request) {
        if (scoreType == ScoreType.THREE_SUBJECT) {
            return request.saitamaDeviationThree() != null
                    ? request.saitamaDeviationThree()
                    : request.threeSubjectDeviation();
        }
        return request.saitamaDeviationFive() != null
                ? request.saitamaDeviationFive()
                : request.fiveSubjectDeviation();
    }

    private String toJudgement(int difference) {
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
