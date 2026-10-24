package com.example.advisor.judgement;

import com.example.advisor.school.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

/** Shared inputs for capturing the old database version and verifying its replacement. */
public final class DatabaseRemovalCases {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HEADER = "name,student_id,times,japanese,math,english,science,socialstudies,"
            + "deviation_japanese,deviation_math,deviation_english,deviation_science,deviation_socialstudies,"
            + "deviation_three,deviation_five,saitama_deviation_three,saitama_deviation_five,"
            + "first_choice,second_choice,third_choice,fourth_choice,fifth_choice\n";

    public static Map<String, String> capture(CsvImportService csv, SchoolRepository schools) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        record(hashes, "schools", () -> new SchoolController(schools).listSchools());
        record(hashes, "courses", () -> new CourseController(schools).listCourses());
        record(hashes, "all-schools", schools::findAllSchools);
        record(hashes, "all-courses", schools::findAllCourses);
        List<Course> courses = schools.findAllCourses();
        for (double delta : new double[]{-5.1, -5, -4.9, -2.1, -2, -1.9, 2.9, 3, 3.1}) {
            StringBuilder rows = new StringBuilder(HEADER);
            for (Course course : courses) {
                int id = Integer.parseInt(course.code().substring(7));
                rows.append("Student ").append(id).append(',').append(100000 + id)
                        .append(",1,80,70,65,60,55,55.1,54.2,53.3,52.4,51.5,59.9,50,77,")
                        .append(String.format(Locale.ROOT, "%.1f", course.deviationValue() + delta))
                        .append(',').append(id).append(",,,,\n");
            }
            String body = rows.toString();
            record(hashes, "all-schools-import-" + delta, () -> csv.importAndJudge(file("all.csv", body)));
            record(hashes, "all-schools-preview-" + delta, () -> csv.preview(new MultipartFile[]{file("all.csv", body)}));
        }

        String basic = "Student,1,1,0,50,50,0,0,50.14,50.15,50.26,0,0,55.7,56.8,65.1,66.2,1,,245,,474\n";
        exercise(hashes, csv, "five-with-holes", HEADER + basic);
        exercise(hashes, csv, "five-fallback", HEADER + basic.replace(",65.1,66.2,", ",65.1,,"));
        exercise(hashes, csv, "average-fallback", HEADER + basic.replace(",56.8,65.1,66.2,", ",,65.1,,"));
        String three = "Student,2,1,50,50,50,,,50.1,51.2,52.3,,,55.7,,65.1,,1,,,,\n";
        exercise(hashes, csv, "three-only", HEADER + three);
        exercise(hashes, csv, "legacy-social-names", HEADER.replace("socialstudies", "socialscience") + basic);
        exercise(hashes, csv, "bom", "\uFEFF" + HEADER + basic);
        exercise(hashes, csv, "old-three-choice-columns", "name,student_id,times,japanese,math,english,"
                + "deviation_japanese,deviation_math,deviation_english,deviation_three,first_choice,second_choice,third_choice\n"
                + "Student,3,1,50,50,50,50.1,51.2,52.3,55.7,1,,245\n");
        exercise(hashes, csv, "no-choices", HEADER + three.replace(",1,,,,\n", ",,,,,\n"));
        exercise(hashes, csv, "duplicate-choice", HEADER + three.replace(",1,,,,\n", ",1,1,,,\n"));
        exercise(hashes, csv, "unknown-choice", HEADER + three.replace(",1,,,,\n", ",2147483647,,,,\n"));
        exercise(hashes, csv, "invalid-number", HEADER + three.replace(",50,50,50,", ",abc,50,50,"));
        exercise(hashes, csv, "missing-column", (HEADER + three).replace("deviation_math", "unused"));
        exercise(hashes, csv, "round-zero", HEADER + three.replace("Student,2,1,", "Student,2,0,"));
        exercise(hashes, csv, "round-eight", HEADER + three.replace("Student,2,1,", "Student,2,8,"));
        exercise(hashes, csv, "empty", "");
        exercise(hashes, csv, "header-only", HEADER);
        String second = three.replace("Student,2,1,", "Student,2,2,");
        record(hashes, "multiple-sorted-deduplicated", () -> csv.preview(new MultipartFile[]{
                file("second.csv", HEADER + second), file("first.csv", HEADER + three), file("copy.csv", HEADER + second)}));
        record(hashes, "multiple-conflicting-score", () -> csv.preview(new MultipartFile[]{
                file("first.csv", HEADER + three), file("conflict.csv", HEADER + three.replace(",50,50,50,", ",51,50,50,"))}));
        record(hashes, "multiple-conflicting-name", () -> csv.preview(new MultipartFile[]{
                file("first.csv", HEADER + three), file("conflict.csv", HEADER + second.replace("Student,", "Other,"))}));
        record(hashes, "later-error-whole-file", () -> csv.importAndJudge(file("later.csv", HEADER + three + three.replace("Student,2,1,", "Other,3,1,").replace(",50,50,50,", ",abc,50,50,"))));
        return hashes;
    }

    private static void exercise(Map<String, String> hashes, CsvImportService csv, String name, String body) throws Exception {
        record(hashes, name + "-import", () -> csv.importAndJudge(file(name + ".csv", body)));
        record(hashes, name + "-preview", () -> csv.preview(new MultipartFile[]{file(name + ".csv", body)}));
    }

    private static MockMultipartFile file(String name, String body) {
        return new MockMultipartFile("file", name, "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void record(Map<String, String> hashes, String name, Supplier<?> call) throws Exception {
        Object result;
        try {
            result = Map.of("status", 200, "body", call.get());
        } catch (IllegalArgumentException ex) {
            result = Map.of("status", 400, "message", ex.getMessage());
        }
        // Canonical object-key order; array order and null slots remain significant.
        String canonical = JSON.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(JSON.convertValue(result, Object.class));
        Path output = Path.of("target", "db-removal-responses", name + ".json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, canonical, StandardCharsets.UTF_8);
        hashes.put(name, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))));
    }
}
