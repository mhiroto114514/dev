package com.example.advisor.school;

public record Course(
        String code,
        String schoolCode,
        String department,
        String courseName,
        ScoreType scoreType,
        int deviationValue
) {
}
