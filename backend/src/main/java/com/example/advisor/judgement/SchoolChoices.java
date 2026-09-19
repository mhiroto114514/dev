package com.example.advisor.judgement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SchoolChoices {
    static final List<String> COLUMNS = List.of("first_choice", "second_choice", "third_choice", "fourth_choice", "fifth_choice");

    private SchoolChoices() {}

    static List<Integer> ids(List<String> codes) {
        if (codes == null || codes.size() > COLUMNS.size()) {
            throw new IllegalArgumentException("志望校は最大5校で指定してください。");
        }
        List<Integer> ids = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < COLUMNS.size(); i++) {
            String code = i < codes.size() ? codes.get(i) : null;
            if (code == null || code.isBlank()) {
                ids.add(null);
                continue;
            }
            Integer id;
            try {
                if (!code.matches("course-[0-9]+")) throw new NumberFormatException();
                id = Integer.valueOf(code.substring(7));
                if (id <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("第" + (i + 1) + "志望の学校IDが不正です。");
            }
            if (!seen.add(id)) throw new IllegalArgumentException("第" + (i + 1) + "志望の学校ID " + id + " が重複しています。");
            ids.add(id);
        }
        return ids;
    }
}
