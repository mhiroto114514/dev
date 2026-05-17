package com.example.advisor.school;

import java.util.List;

public record SchoolListResponse(
        List<SchoolSummary> publicSchools,
        List<SchoolSummary> privateSchools,
        List<SchoolSummary> nationalSchools,
        List<SchoolSummary> kosenSchools
) {
}
