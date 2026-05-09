package com.example.advisor.school;

import java.util.List;

public record CourseListResponse(
        List<CourseSummary> publicCourses,
        List<CourseSummary> privateCourses
) {
}
