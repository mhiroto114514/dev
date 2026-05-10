package com.example.advisor.school;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolRepository schoolRepository;

    public SchoolController(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @GetMapping
    public SchoolListResponse listSchools() {
        return new SchoolListResponse(
                schoolRepository.findSchoolsByCategory(SchoolCategory.PUBLIC).stream()
                        .map(this::toSummary)
                        .toList(),
                schoolRepository.findSchoolsByCategory(SchoolCategory.PRIVATE).stream()
                        .map(this::toSummary)
                        .toList()
        );
    }

    private SchoolSummary toSummary(School school) {
        return new SchoolSummary(
                school.code(),
                school.name(),
                school.schoolCategory(),
                ""
        );
    }
}
