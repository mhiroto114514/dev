package com.example.advisor.judgement;

import com.example.advisor.school.ScoreType;
import com.example.advisor.school.SchoolCategory;

public record SchoolJudgementResult(
        String schoolName,
        String department,
        String courseName,
        String area,
        SchoolCategory schoolCategory,
        String judgement,
        ScoreType usedScoreType,
        double studentDeviationValue,
        double targetDeviationValue,
        double difference
) {
}
