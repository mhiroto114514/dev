package com.example.advisor.school;

public record SchoolSummary(
        String code,
        String name,
        SchoolCategory schoolCategory,
        String area
) {
}
