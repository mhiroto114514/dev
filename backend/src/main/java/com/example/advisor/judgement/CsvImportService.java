package com.example.advisor.judgement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvImportService {

    private final JudgementService judgementService;

    public CsvImportService(JudgementService judgementService) {
        this.judgementService = judgementService;
    }

    @Transactional
    public CsvImportResponse importAndJudge(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a CSV file.");
        }

        List<StudentLedgerResult> ledgers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV header is missing.");
            }

            Map<String, Integer> headerIndex = toHeaderIndex(headerLine);
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                ledgers.add(parseAndJudgeLine(line, lineNumber, headerIndex));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read CSV file.");
        }

        if (ledgers.isEmpty()) {
            throw new IllegalArgumentException("No data rows were found in CSV.");
        }

        return new CsvImportResponse(ledgers);
    }

    private StudentLedgerResult parseAndJudgeLine(String line, int lineNumber, Map<String, Integer> headerIndex) {
        String[] cells = line.split(",", -1);
        String studentName = getRequiredCell(cells, headerIndex, lineNumber, "name");
        int studentCode = parseInt(getRequiredCell(cells, headerIndex, lineNumber, "student_id"), lineNumber, "student_id");
        int times = parseInt(getRequiredCell(cells, headerIndex, lineNumber, "times"), lineNumber, "times");
        int japanese = parseInt(getRequiredCell(cells, headerIndex, lineNumber, "japanese"), lineNumber, "japanese");
        int math = parseInt(getRequiredCell(cells, headerIndex, lineNumber, "math"), lineNumber, "math");
        int english = parseInt(getRequiredCell(cells, headerIndex, lineNumber, "english"), lineNumber, "english");

        Integer science = parseNullableInt(
                getOptionalCellIfPresent(cells, headerIndex, "science"),
                lineNumber,
                "science"
        );
        Integer socialstudies = parseNullableInt(
                getOptionalCellIfPresent(cells, headerIndex, "socialstudies", "socialscience"),
                lineNumber,
                "socialstudies"
        );

        int deviationJapanese = parseRoundedInt(
                getRequiredCell(cells, headerIndex, lineNumber, "deviation_japanese"),
                lineNumber,
                "deviation_japanese"
        );
        int deviationMath = parseRoundedInt(
                getRequiredCell(cells, headerIndex, lineNumber, "deviation_math"),
                lineNumber,
                "deviation_math"
        );
        int deviationEnglish = parseRoundedInt(
                getRequiredCell(cells, headerIndex, lineNumber, "deviation_english"),
                lineNumber,
                "deviation_english"
        );
        Integer deviationScience = parseNullableRoundedInt(
                getOptionalCellIfPresent(cells, headerIndex, "deviation_science"),
                lineNumber,
                "deviation_science"
        );
        Integer deviationSocialstudies = parseNullableRoundedInt(
                getOptionalCellIfPresent(cells, headerIndex, "deviation_socialstudies", "deviation_socialscience"),
                lineNumber,
                "deviation_socialstudies"
        );
        int deviationThree = parseRoundedInt(
                getRequiredCell(cells, headerIndex, lineNumber, "deviation_three"),
                lineNumber,
                "deviation_three"
        );
        Integer deviationFive = parseNullableRoundedInt(
                getOptionalCellIfPresent(cells, headerIndex, "deviation_five"),
                lineNumber,
                "deviation_five"
        );
        Integer saitamaDeviationThree = parseNullableRoundedInt(
                getOptionalCellIfPresent(cells, headerIndex, "saitama_deviation_three"),
                lineNumber,
                "saitama_deviation_three"
        );
        Integer saitamaDeviationFive = parseNullableRoundedInt(
                getOptionalCellIfPresent(cells, headerIndex, "saitama_deviation_five"),
                lineNumber,
                "saitama_deviation_five"
        );

        List<String> desiredCourseCodes = new ArrayList<>();
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "first_choice"), lineNumber, "first_choice");
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "second_choice"), lineNumber, "second_choice");
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "third_choice"), lineNumber, "third_choice");

        if (desiredCourseCodes.isEmpty()) {
            throw new IllegalArgumentException("CSV line " + lineNumber + ": at least one school choice is required.");
        }

        JudgementRequest request = new JudgementRequest(
                studentCode,
                studentName,
                times,
                japanese,
                math,
                english,
                science,
                socialstudies,
                deviationJapanese,
                deviationMath,
                deviationEnglish,
                deviationScience,
                deviationSocialstudies,
                deviationThree,
                deviationFive,
                saitamaDeviationThree,
                saitamaDeviationFive,
                desiredCourseCodes
        );

        JudgementResponse response = judgementService.judge(request);

        return new StudentLedgerResult(
                studentCode,
                studentName,
                times,
                japanese,
                math,
                english,
                science,
                socialstudies,
                deviationJapanese,
                deviationMath,
                deviationEnglish,
                deviationScience,
                deviationSocialstudies,
                deviationThree,
                deviationFive,
                saitamaDeviationThree,
                saitamaDeviationFive,
                response.results()
        );
    }

    private Map<String, Integer> toHeaderIndex(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String key = headers[i].replace("\uFEFF", "").trim().toLowerCase();
            index.put(key, i);
        }
        return index;
    }

    private String getRequiredCell(String[] cells, Map<String, Integer> headerIndex, int lineNumber, String... names) {
        String value = getOptionalCell(cells, headerIndex, names);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CSV line " + lineNumber + ": " + names[0] + " is required.");
        }
        return value;
    }

    private String getOptionalCell(String[] cells, Map<String, Integer> headerIndex, String... names) {
        Integer idx = resolveHeaderIndex(headerIndex, names);
        if (idx == null) {
            throw new IllegalArgumentException("CSV header is missing column: " + names[0]);
        }
        if (idx >= cells.length) {
            return "";
        }
        return cells[idx].trim();
    }

    private String getOptionalCellIfPresent(String[] cells, Map<String, Integer> headerIndex, String... names) {
        Integer idx = resolveHeaderIndex(headerIndex, names);
        if (idx == null || idx >= cells.length) {
            return null;
        }
        String value = cells[idx].trim();
        return value.isBlank() ? null : value;
    }

    private Integer resolveHeaderIndex(Map<String, Integer> headerIndex, String... names) {
        for (String name : names) {
            Integer candidate = headerIndex.get(name.toLowerCase());
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private int parseInt(String raw, int lineNumber, String column) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CSV line " + lineNumber + ": invalid integer in " + column);
        }
    }

    private int parseRoundedInt(String raw, int lineNumber, String column) {
        try {
            return (int) Math.round(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CSV line " + lineNumber + ": invalid number in " + column);
        }
    }

    private Integer parseNullableInt(String raw, int lineNumber, String column) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseInt(raw, lineNumber, column);
    }

    private Integer parseNullableRoundedInt(String raw, int lineNumber, String column) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseRoundedInt(raw, lineNumber, column);
    }

    private void appendCourseCode(List<String> desiredCourseCodes, String raw, int lineNumber, String column) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        int schoolId = parseInt(raw, lineNumber, column);
        desiredCourseCodes.add("course-" + schoolId);
    }
}
