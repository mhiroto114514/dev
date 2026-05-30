package com.example.advisor.judgement;

import com.example.advisor.db.DeviationSchemaMigrationService;
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
    private final DeviationSchemaMigrationService deviationSchemaMigrationService;
    private volatile ResultTableColumns resultTableColumns;

    public JudgementPersistenceService(
            JdbcTemplate jdbcTemplate,
            DeviationSchemaMigrationService deviationSchemaMigrationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.deviationSchemaMigrationService = deviationSchemaMigrationService;
    }

    @Transactional
    public void save(JudgementRequest request, Integer firstChoice, Integer secondChoice, Integer thirdChoice) {
        deviationSchemaMigrationService.ensureMigrated();
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
        List<Object> params = new ArrayList<>();
        params.add(studentPk);
        params.add(request.times());
        params.add(request.japaneseScore());
        params.add(request.mathScore());
        params.add(request.englishScore());
        params.add(request.scienceScore());
        params.add(request.socialstudiesScore());
        params.add(request.japaneseDeviation());
        params.add(request.mathDeviation());
        params.add(request.englishDeviation());
        params.add(request.scienceDeviation());
        params.add(request.socialstudiesDeviation());
        params.add(request.threeSubjectDeviation());
        params.add(request.fiveSubjectDeviation());

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
