package com.example.advisor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://127.0.0.1:1/not-used", "spring.sql.init.mode=always"})
@AutoConfigureMockMvc
class DatabaseFreeApplicationTest {
    @Autowired ApplicationContext context;
    @Autowired MockMvc http;

    @Test void startsWithoutAnyDatabaseOrDriver() {
        assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
        for (String type : new String[]{"org.postgresql.Driver", "org.h2.Driver", "com.zaxxer.hikari.HikariDataSource", "org.springframework.jdbc.core.JdbcTemplate"}) {
            assertFalse(ClassUtils.isPresent(type, getClass().getClassLoader()), type);
        }
    }

    @Test void csvEndpointsWorkWithoutDatabaseAndKeepValidation() throws Exception {
        String header = "name,student_id,times,japanese,math,english,deviation_japanese,deviation_math,deviation_english,deviation_three,first_choice,second_choice,third_choice\n";
        String row = "Student,1,1,50,50,50,50.1,51.2,52.3,55.7,1,,\n";
        for (String endpoint : new String[]{"csv-import", "csv-preview"}) {
            String field = endpoint.equals("csv-import") ? "file" : "files";
            http.perform(multipart("/api/judgements/" + endpoint).file(new MockMultipartFile(field, "test.csv", "text/csv", (header + row).getBytes(StandardCharsets.UTF_8))))
                    .andExpect(status().isOk()).andExpect(jsonPath("ledgers[0].studentName").value("Student"))
                    .andExpect(jsonPath("ledgers[0].results", hasSize(5)))
                    .andExpect(jsonPath("ledgers[0].results[0].targetDeviationValue").value(72.0));
        }
        http.perform(multipart("/api/judgements/csv-import").file(new MockMultipartFile("file", "bad.csv", "text/csv", (header + row.replace(",1,,", ",2147483647,,")).getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").exists());
    }
}
