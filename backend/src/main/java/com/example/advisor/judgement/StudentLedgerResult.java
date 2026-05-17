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
        Integer japaneseDeviation,
        Integer mathDeviation,
        Integer englishDeviation,
        Integer scienceDeviation,
        Integer socialstudiesDeviation,
        Integer threeSubjectDeviation,
        Integer fiveSubjectDeviation,
        Integer saitamaDeviationThree,
        Integer saitamaDeviationFive,
        List<SchoolJudgementResult> results
) {
}

