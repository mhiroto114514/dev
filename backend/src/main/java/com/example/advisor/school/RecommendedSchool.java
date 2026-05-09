package com.example.advisor.school;

public record RecommendedSchool(
        String courseCode,
        String schoolName,
        String department,
        String courseName,
        SchoolCategory schoolCategory,
        ScoreType scoreType,
        int deviationValue,
        String area,
        RecommendationType recommendationType
) {
}
