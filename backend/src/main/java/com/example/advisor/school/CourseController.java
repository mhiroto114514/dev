package com.example.advisor.school;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final SchoolRepository schoolRepository;

    public CourseController(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @GetMapping
    public CourseListResponse listCourses() {
        return new CourseListResponse(
                schoolRepository.findCoursesByCategory(SchoolCategory.PUBLIC).stream()
                        .map(this::toSummary)
                        .toList(),
                schoolRepository.findCoursesByCategory(SchoolCategory.PRIVATE).stream()
                        .map(this::toSummary)
                        .toList(),
                schoolRepository.findCoursesByCategory(SchoolCategory.NATIONAL).stream()
                        .map(this::toSummary)
                        .toList(),
                schoolRepository.findCoursesByCategory(SchoolCategory.KOSEN).stream()
                        .map(this::toSummary)
                        .toList()
        );
    }

    private CourseSummary toSummary(Course course) {
        School school = schoolRepository.findSchoolByCode(course.schoolCode())
                .orElseThrow();

        return new CourseSummary(
                course.code(),
                school.code(),
                school.name(),
                course.department(),
                course.courseName(),
                school.schoolCategory(),
                course.scoreType(),
                course.deviationValue(),
                ""
        );
    }
}
