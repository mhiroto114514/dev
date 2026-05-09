package com.example.advisor.school;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

@Repository
public class SchoolRepository {

    private final List<School> schools;
    private final List<Course> courses;

    public SchoolRepository(ObjectMapper objectMapper) {
        this.schools = loadSchools(objectMapper);
        this.courses = loadCourses(objectMapper);
    }

    public List<School> findAllSchools() {
        return schools;
    }

    public List<School> findSchoolsByCategory(SchoolCategory schoolCategory) {
        return schools.stream()
                .filter(school -> school.schoolCategory() == schoolCategory)
                .toList();
    }

    public Optional<School> findSchoolByCode(String schoolCode) {
        return schools.stream()
                .filter(school -> school.code().equals(schoolCode))
                .findFirst();
    }

    public List<Course> findAllCourses() {
        return courses;
    }

    public List<Course> findCoursesByCategory(SchoolCategory schoolCategory) {
        return courses.stream()
                .filter(course -> findSchoolByCode(course.schoolCode())
                        .map(School::schoolCategory)
                        .filter(category -> category == schoolCategory)
                        .isPresent())
                .toList();
    }

    public Optional<Course> findCourseByCode(String courseCode) {
        return courses.stream()
                .filter(course -> course.code().equals(courseCode))
                .findFirst();
    }

    private List<School> loadSchools(ObjectMapper objectMapper) {
        return readList(objectMapper, "data/schools.json", new TypeReference<>() {
        });
    }

    private List<Course> loadCourses(ObjectMapper objectMapper) {
        return readList(objectMapper, "data/courses.json", new TypeReference<>() {
        });
    }

    private <T> List<T> readList(ObjectMapper objectMapper, String path, TypeReference<List<T>> typeReference) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load resource: " + path, e);
        }
    }
}
