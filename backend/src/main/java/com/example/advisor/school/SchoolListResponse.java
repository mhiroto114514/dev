package com.example.advisor.school;

import java.util.List;

public record SchoolListResponse(
        List<SchoolSummary> publicSchools,
        List<SchoolSummary> privateSchools
) {
}
