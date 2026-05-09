package com.example.advisor.judgement;

import com.example.advisor.school.RecommendedSchool;
import com.example.advisor.school.ScoreType;
import com.example.advisor.school.SchoolCategory;

import java.util.List;

public record SchoolJudgementResult(
        String schoolName,
        String department,
        String courseName,
        String area,
        SchoolCategory schoolCategory,
        String judgement,
        ScoreType usedScoreType,
        int studentDeviationValue,
        int targetDeviationValue,
        int difference,
        List<RecommendedSchool> recommendedSchools
) {
}
