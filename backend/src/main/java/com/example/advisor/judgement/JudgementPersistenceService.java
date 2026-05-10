package com.example.advisor.judgement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudgementPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public JudgementPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(JudgementRequest request, Integer firstChoice, Integer secondChoice, Integer thirdChoice) {
        Integer studentPk = jdbcTemplate.query(
                "SELECT id FROM student WHERE student_id = ?",
                ps -> ps.setInt(1, request.studentCode()),
                rs -> rs.next() ? rs.getInt("id") : null
        );

        if (studentPk == null) {
            jdbcTemplate.update(
                    "INSERT INTO student (student_id, name) VALUES (?, ?)",
                    request.studentCode(),
                    "student-" + request.studentCode()
            );
            studentPk = jdbcTemplate.queryForObject(
                    "SELECT id FROM student WHERE student_id = ?",
                    Integer.class,
                    request.studentCode()
            );
        }

        jdbcTemplate.update(
                """
                INSERT INTO result (
                    student_id, times, japanese, math, english, science, socialscience,
                    deviation_japanese, deviation_math, deviation_english, deviation_science, deviation_socialscience,
                    deviation_three, deviation_five, first_choice, second_choice, third_choice
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                studentPk,
                request.times(),
                request.japaneseScore(),
                request.mathScore(),
                request.englishScore(),
                request.scienceScore(),
                request.socialscienceScore(),
                request.japaneseDeviation(),
                request.mathDeviation(),
                request.englishDeviation(),
                request.scienceDeviation(),
                request.socialscienceDeviation(),
                request.threeSubjectDeviation(),
                request.fiveSubjectDeviation(),
                firstChoice,
                secondChoice,
                thirdChoice
        );
    }
}
