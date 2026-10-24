package com.example.advisor.school;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Repository
public class SchoolRepository {
    private final List<Entry> sorted;
    private final Map<Integer, Entry> byId;

    public SchoolRepository(@Value("${advisor.school-master:classpath:school-master.csv}") Resource resource) {
        Map<Integer, Entry> loaded = new HashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.replace("\uFEFF", "").equals("id,name,deviation,school_category")) {
                throw new IllegalArgumentException("Expected id,name,deviation,school_category header");
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    List<String> cells = cells(line);
                    if (cells.size() != 4) throw new IllegalArgumentException("Expected four columns");
                    int id = Integer.parseInt(cells.get(0).trim());
                    if (id <= 0) throw new IllegalArgumentException("ID must be positive");
                    String name = cells.get(1);
                    if (name.isBlank() || name.codePointCount(0, name.length()) > 120) {
                        throw new IllegalArgumentException("Name must have 1..120 characters");
                    }
                    // Match the former NUMERIC(4,1) column and its rounding.
                    BigDecimal deviation = new BigDecimal(cells.get(2).trim()).setScale(1, RoundingMode.HALF_UP);
                    if (deviation.abs().compareTo(new BigDecimal("999.9")) > 0) throw new IllegalArgumentException("Deviation out of range");
                    SchoolCategory category = SchoolCategory.valueOf(cells.get(3).trim());
                    if (loaded.putIfAbsent(id, new Entry(id, name, deviation.doubleValue(), category)) != null) {
                        throw new IllegalArgumentException("Duplicate ID " + id);
                    }
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Line " + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
            if (loaded.isEmpty()) throw new IllegalArgumentException("School master is empty");
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot load school master " + resource.getDescription() + ": " + ex.getMessage(), ex);
        }
        byId = Map.copyOf(loaded);
        sorted = loaded.values().stream().sorted(Comparator.comparingDouble(Entry::deviation).reversed()
                .thenComparingInt(Entry::id)).toList();
    }

    public List<School> findAllSchools() { return sorted.stream().map(Entry::school).toList(); }
    public List<Course> findAllCourses() { return sorted.stream().map(Entry::course).toList(); }
    public List<School> findSchoolsByCategory(SchoolCategory category) {
        return sorted.stream().filter(e -> e.category() == category).map(Entry::school).toList();
    }
    public List<Course> findCoursesByCategory(SchoolCategory category) {
        return sorted.stream().filter(e -> e.category() == category).map(Entry::course).toList();
    }
    public Optional<School> findSchoolByCode(String code) {
        return Optional.ofNullable(byId.get(id(code, "school-", "School"))).map(Entry::school);
    }
    public Optional<Course> findCourseByCode(String code) {
        return Optional.ofNullable(byId.get(id(code, "course-", "Course"))).map(Entry::course);
    }
    private static int id(String code, String prefix, String label) {
        if (code == null || !code.startsWith(prefix)) throw new IllegalArgumentException(label + " code format is invalid.");
        return Integer.parseInt(code.substring(prefix.length()));
    }
    private record Entry(int id, String name, double deviation, SchoolCategory category) {
        School school() { return new School("school-" + id, name, category); }
        Course course() {
            // Keep the existing course interpretation; names already contain the course label.
            return new Course("course-" + id, "school-" + id, "普通科", name, ScoreType.FIVE_SUBJECT, deviation);
        }
    }

    private static List<String> cells(String line) {
        List<String> result = new ArrayList<>();
        int i = 0;
        do {
            StringBuilder value = new StringBuilder();
            if (i < line.length() && line.charAt(i) == '"') {
                i++;
                boolean closed = false;
                while (i < line.length()) {
                    char c = line.charAt(i++);
                    if (c != '"') value.append(c);
                    else if (i < line.length() && line.charAt(i) == '"') { value.append('"'); i++; }
                    else { closed = true; break; }
                }
                if (!closed || (i < line.length() && line.charAt(i) != ',')) throw new IllegalArgumentException("Invalid CSV quoting");
            } else {
                while (i < line.length() && line.charAt(i) != ',') {
                    char c = line.charAt(i++);
                    if (c == '"') throw new IllegalArgumentException("Invalid CSV quoting");
                    value.append(c);
                }
            }
            result.add(value.toString());
            if (i == line.length()) break;
            i++;
            if (i == line.length()) { result.add(""); break; }
        } while (true);
        return result;
    }
}
