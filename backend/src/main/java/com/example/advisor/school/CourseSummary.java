package com.example.advisor.school;

public record CourseSummary(
        String code,
        String schoolCode,
        String schoolName,
        String department,
        String courseName,
        SchoolCategory schoolCategory,
        ScoreType scoreType,
        double deviationValue,
        String area
) {
}
