package com.example.advisor.school;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SchoolRepository {

    private static final SchoolCategory DEFAULT_CATEGORY = SchoolCategory.PUBLIC;
    private static final String DEFAULT_DEPARTMENT = "普通科";

    private final JdbcTemplate jdbcTemplate;

    public SchoolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<School> findAllSchools() {
        String sql = "SELECT id, name FROM school ORDER BY deviation DESC, id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new School(
                toSchoolCode(rs.getInt("id")),
                rs.getString("name"),
                DEFAULT_CATEGORY
        ));
    }

    public List<School> findSchoolsByCategory(SchoolCategory schoolCategory) {
        if (schoolCategory != DEFAULT_CATEGORY) {
            return List.of();
        }
        return findAllSchools();
    }

    public Optional<School> findSchoolByCode(String schoolCode) {
        String sql = "SELECT id, name FROM school WHERE id = ?";
        List<School> schools = jdbcTemplate.query(sql, (rs, rowNum) -> new School(
                        toSchoolCode(rs.getInt("id")),
                        rs.getString("name"),
                        DEFAULT_CATEGORY
                ),
                fromSchoolCode(schoolCode)
        );
        return schools.stream().findFirst();
    }

    public List<Course> findAllCourses() {
        String sql = "SELECT id, name, deviation FROM school ORDER BY deviation DESC, id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> toCourse(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("deviation")
        ));
    }

    public List<Course> findCoursesByCategory(SchoolCategory schoolCategory) {
        if (schoolCategory != DEFAULT_CATEGORY) {
            return List.of();
        }
        return findAllCourses();
    }

    public Optional<Course> findCourseByCode(String courseCode) {
        String schoolCode = fromCourseCode(courseCode);
        String sql = "SELECT id, name, deviation FROM school WHERE id = ?";
        List<Course> courses = jdbcTemplate.query(sql, (rs, rowNum) -> toCourse(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("deviation")
                ),
                fromSchoolCode(schoolCode)
        );
        return courses.stream().findFirst();
    }

    private Course toCourse(int schoolId, String schoolName, int deviation) {
        return new Course(
                toCourseCode(schoolId),
                toSchoolCode(schoolId),
                DEFAULT_DEPARTMENT,
                schoolName,
                ScoreType.FIVE_SUBJECT,
                deviation
        );
    }

    private String toSchoolCode(int id) {
        return "school-" + id;
    }

    private String toCourseCode(int id) {
        return "course-" + id;
    }

    private int fromSchoolCode(String schoolCode) {
        if (schoolCode == null || !schoolCode.startsWith("school-")) {
            throw new IllegalArgumentException("学校コードの形式が不正です。");
        }
        return Integer.parseInt(schoolCode.substring("school-".length()));
    }

    private String fromCourseCode(String courseCode) {
        if (courseCode == null || !courseCode.startsWith("course-")) {
            throw new IllegalArgumentException("コースコードの形式が不正です。");
        }
        return "school-" + Integer.parseInt(courseCode.substring("course-".length()));
    }
}
