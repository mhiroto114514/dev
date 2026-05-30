package com.example.advisor.judgement;

import java.util.List;

public record StudentLedgerResult(
        Integer studentCode,
        String studentName,
        Integer times,
        Integer japaneseScore,
        Integer mathScore,
        Integer englishScore,
        Integer scienceScore,
        Integer socialstudiesScore,
        Double japaneseDeviation,
        Double mathDeviation,
        Double englishDeviation,
        Double scienceDeviation,
        Double socialstudiesDeviation,
        Double threeSubjectDeviation,
        Double fiveSubjectDeviation,
        Double saitamaDeviationThree,
        Double saitamaDeviationFive,
        List<SchoolJudgementResult> results
) {
}
