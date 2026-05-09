package com.example.advisor.judgement;

import java.util.List;

public record JudgementResponse(
        List<SchoolJudgementResult> results
) {
}
