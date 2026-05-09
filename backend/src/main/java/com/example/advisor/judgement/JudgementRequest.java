package com.example.advisor.judgement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JudgementRequest(
        @NotNull @Min(20) @Max(90) Integer threeSubjectDeviation,
        @NotNull @Min(20) @Max(90) Integer fiveSubjectDeviation,
        @NotNull @Size(max = 3) List<String> publicDesiredCourseCodes,
        @NotNull @Size(max = 3) List<String> privateDesiredCourseCodes
) {
}
