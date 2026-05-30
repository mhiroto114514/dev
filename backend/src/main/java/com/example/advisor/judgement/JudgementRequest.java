package com.example.advisor.judgement;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @Min(0) @Max(100) Integer scienceScore,
        @JsonAlias("socialscienceScore")
        @Min(0) @Max(100) Integer socialstudiesScore,
        @NotNull @DecimalMin("20.0") @DecimalMax("90.0") Double japaneseDeviation,
        @NotNull @DecimalMin("20.0") @DecimalMax("90.0") Double mathDeviation,
        @NotNull @DecimalMin("20.0") @DecimalMax("90.0") Double englishDeviation,
        @DecimalMin("20.0") @DecimalMax("90.0") Double scienceDeviation,
        @JsonAlias("socialscienceDeviation")
        @DecimalMin("20.0") @DecimalMax("90.0") Double socialstudiesDeviation,
        @NotNull @DecimalMin("20.0") @DecimalMax("90.0") Double threeSubjectDeviation,
        @DecimalMin("20.0") @DecimalMax("90.0") Double fiveSubjectDeviation,
        Double saitamaDeviationThree,
        Double saitamaDeviationFive,
        @NotNull @Size(max = 3) List<String> desiredCourseCodes
) {
}
