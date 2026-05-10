package com.example.advisor.student;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final JdbcTemplate jdbcTemplate;

    public StudentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{studentCode}")
    public StudentLookupResponse findByStudentCode(@PathVariable Integer studentCode) {
        String name = jdbcTemplate.query(
                "SELECT name FROM student WHERE student_id = ?",
                ps -> ps.setInt(1, studentCode),
                rs -> rs.next() ? rs.getString("name") : null
        );

        if (name == null) {
            throw new IllegalArgumentException("該当する生徒が見つかりません。");
        }

        return new StudentLookupResponse(studentCode, name);
    }
}
