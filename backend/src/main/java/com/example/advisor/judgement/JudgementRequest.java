package com.example.advisor.judgement;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JudgementRequest(
        @NotNull @Min(1) Integer studentCode,
        String studentName,
        @NotNull @Min(1) Integer times,
        @NotNull @Min(0) @Max(100) Integer japaneseScore,
        @NotNull @Min(0) @Max(100) Integer mathScore,
        @NotNull @Min(0) @Max(100) Integer englishScore,
        @NotNull @Min(0) @Max(100) Integer scienceScore,
        @JsonAlias("socialscienceScore")
        @NotNull @Min(0) @Max(100) Integer socialstudiesScore,
        @NotNull @Min(20) @Max(90) Integer japaneseDeviation,
        @NotNull @Min(20) @Max(90) Integer mathDeviation,
        @NotNull @Min(20) @Max(90) Integer englishDeviation,
        @NotNull @Min(20) @Max(90) Integer scienceDeviation,
        @JsonAlias("socialscienceDeviation")
        @NotNull @Min(20) @Max(90) Integer socialstudiesDeviation,
        @NotNull @Min(20) @Max(90) Integer threeSubjectDeviation,
        @NotNull @Min(20) @Max(90) Integer fiveSubjectDeviation,
        Integer saitamaDeviationThree,
        Integer saitamaDeviationFive,
        @NotNull @Size(max = 3) List<String> desiredCourseCodes
) {
}
