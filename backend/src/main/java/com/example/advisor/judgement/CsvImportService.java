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
            throw new IllegalArgumentException("CSVファイルを選択してください。");
        }

        List<StudentLedgerResult> ledgers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSVヘッダーが見つかりません。");
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
            throw new IllegalArgumentException("CSVの読み込みに失敗しました。");
        }

        if (ledgers.isEmpty()) {
            throw new IllegalArgumentException("CSVに取り込み対象データがありません。");
        }

        return new CsvImportResponse(ledgers);
    }

    private StudentLedgerResult parseAndJudgeLine(String line, int lineNumber, Map<String, Integer> headerIndex) {
        String[] cells = line.split(",", -1);
        String studentName = getCell(cells, headerIndex, lineNumber, "name");
        int studentCode = parseInt(getCell(cells, headerIndex, lineNumber, "student_id"), lineNumber, "student_id");
        int times = parseInt(getCell(cells, headerIndex, lineNumber, "times"), lineNumber, "times");
        int japanese = parseInt(getCell(cells, headerIndex, lineNumber, "japanese"), lineNumber, "japanese");
        int math = parseInt(getCell(cells, headerIndex, lineNumber, "math"), lineNumber, "math");
        int english = parseInt(getCell(cells, headerIndex, lineNumber, "english"), lineNumber, "english");
        int science = parseInt(getCell(cells, headerIndex, lineNumber, "science"), lineNumber, "science");
        int socialstudies = parseInt(
                getCell(cells, headerIndex, lineNumber, "socialstudies", "socialscience"),
                lineNumber,
                "socialstudies"
        );

        int deviationJapanese = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_japanese"),
                lineNumber,
                "deviation_japanese"
        );
        int deviationMath = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_math"),
                lineNumber,
                "deviation_math"
        );
        int deviationEnglish = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_english"),
                lineNumber,
                "deviation_english"
        );
        int deviationScience = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_science"),
                lineNumber,
                "deviation_science"
        );
        int deviationSocialstudies = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_socialstudies", "deviation_socialscience"),
                lineNumber,
                "deviation_socialstudies"
        );
        int deviationThree = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_three"),
                lineNumber,
                "deviation_three"
        );
        int deviationFive = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "deviation_five"),
                lineNumber,
                "deviation_five"
        );
        int saitamaDeviationThree = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "saitama_deviation_three"),
                lineNumber,
                "saitama_deviation_three"
        );
        int saitamaDeviationFive = parseRoundedInt(
                getCell(cells, headerIndex, lineNumber, "saitama_deviation_five"),
                lineNumber,
                "saitama_deviation_five"
        );

        List<String> desiredCourseCodes = new ArrayList<>();
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "first_choice"), lineNumber, "first_choice");
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "second_choice"), lineNumber, "second_choice");
        appendCourseCode(desiredCourseCodes, getOptionalCell(cells, headerIndex, "third_choice"), lineNumber, "third_choice");

        if (desiredCourseCodes.isEmpty()) {
            throw new IllegalArgumentException("CSV " + lineNumber + "行目: 志望校を1校以上指定してください。");
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

    private String getCell(String[] cells, Map<String, Integer> headerIndex, int lineNumber, String... names) {
        String value = getOptionalCell(cells, headerIndex, names);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CSV " + lineNumber + "行目: " + names[0] + " が空です。");
        }
        return value;
    }

    private String getOptionalCell(String[] cells, Map<String, Integer> headerIndex, String... names) {
        Integer idx = null;
        for (String name : names) {
            Integer candidate = headerIndex.get(name.toLowerCase());
            if (candidate != null) {
                idx = candidate;
                break;
            }
        }
        if (idx == null) {
            throw new IllegalArgumentException("CSVヘッダーに " + names[0] + " がありません。");
        }
        if (idx >= cells.length) {
            return "";
        }
        return cells[idx].trim();
    }

    private int parseInt(String raw, int lineNumber, String column) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CSV " + lineNumber + "行目: " + column + " の値が不正です。");
        }
    }

    private int parseRoundedInt(String raw, int lineNumber, String column) {
        try {
            return (int) Math.round(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CSV " + lineNumber + "行目: " + column + " の値が不正です。");
        }
    }

    private void appendCourseCode(List<String> desiredCourseCodes, String raw, int lineNumber, String column) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        int schoolId = parseInt(raw, lineNumber, column);
        desiredCourseCodes.add("course-" + schoolId);
    }
}
