package com.example.advisor.judgement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JudgementPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private volatile ResultTableColumns resultTableColumns;

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
            String studentName = request.studentName() == null || request.studentName().isBlank()
                    ? "student-" + request.studentCode()
                    : request.studentName().trim();
            jdbcTemplate.update(
                    "INSERT INTO student (student_id, name) VALUES (?, ?)",
                    request.studentCode(),
                    studentName
            );
            studentPk = jdbcTemplate.queryForObject(
                    "SELECT id FROM student WHERE student_id = ?",
                    Integer.class,
                    request.studentCode()
            );
        }

        ResultTableColumns columns = getResultTableColumns();

        List<String> insertColumns = new ArrayList<>(List.of(
                "student_id",
                "times",
                "japanese",
                "math",
                "english",
                "science",
                columns.socialScoreColumn(),
                "deviation_japanese",
                "deviation_math",
                "deviation_english",
                "deviation_science",
                columns.socialDeviationColumn(),
                "deviation_three",
                "deviation_five"
        ));
        List<Object> params = new ArrayList<>(List.of(
                studentPk,
                request.times(),
                request.japaneseScore(),
                request.mathScore(),
                request.englishScore(),
                request.scienceScore(),
                request.socialstudiesScore(),
                request.japaneseDeviation(),
                request.mathDeviation(),
                request.englishDeviation(),
                request.scienceDeviation(),
                request.socialstudiesDeviation(),
                request.threeSubjectDeviation(),
                request.fiveSubjectDeviation()
        ));

        if (columns.hasSaitamaDeviationThree()) {
            insertColumns.add("saitama_deviation_three");
            params.add(request.saitamaDeviationThree());
        }
        if (columns.hasSaitamaDeviationFive()) {
            insertColumns.add("saitama_deviation_five");
            params.add(request.saitamaDeviationFive());
        }

        insertColumns.add("first_choice");
        insertColumns.add("second_choice");
        insertColumns.add("third_choice");
        params.add(firstChoice);
        params.add(secondChoice);
        params.add(thirdChoice);

        String placeholders = insertColumns.stream().map(column -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO result (" + String.join(", ", insertColumns) + ") VALUES (" + placeholders + ")";
        jdbcTemplate.update(sql, params.toArray());
    }

    private ResultTableColumns getResultTableColumns() {
        ResultTableColumns current = resultTableColumns;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (resultTableColumns == null) {
                Set<String> resultColumns = jdbcTemplate.queryForList(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'result'
                        """,
                        String.class
                ).stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                resultTableColumns = new ResultTableColumns(
                        resolveColumn(resultColumns, "socialstudies", "socialscience"),
                        resolveColumn(resultColumns, "deviation_socialstudies", "deviation_socialscience"),
                        resultColumns.contains("saitama_deviation_three"),
                        resultColumns.contains("saitama_deviation_five")
                );
            }
            return resultTableColumns;
        }
    }

    private String resolveColumn(Set<String> columns, String preferred, String legacy) {
        if (columns.contains(preferred)) {
            return preferred;
        }
        if (columns.contains(legacy)) {
            return legacy;
        }
        throw new IllegalStateException("resultテーブルに必要な列がありません: " + preferred + " または " + legacy);
    }

    private record ResultTableColumns(
            String socialScoreColumn,
            String socialDeviationColumn,
            boolean hasSaitamaDeviationThree,
            boolean hasSaitamaDeviationFive
    ) {
    }
}
