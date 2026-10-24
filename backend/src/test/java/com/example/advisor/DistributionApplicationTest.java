package com.example.advisor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "advisor.distribution", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "spring.config.location=classpath:/application.properties"})
class DistributionApplicationTest {
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @Test void servesFrontendAssetsAndApiFromOnePort() {
        var response = http.getForEntity("http://127.0.0.1:" + port + "/", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("<div id=\"root\"></div>"));
        assertFalse(response.getBody().contains("fonts.googleapis.com"));
        var assets = Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(response.getBody());
        int count = 0;
        while (assets.find()) {
            var asset = http.getForEntity(assets.group(1), String.class);
            assertEquals(200, asset.getStatusCode().value());
            if (assets.group(1).endsWith(".js")) {
                assertTrue(asset.getBody().contains("/api/judgements/csv-import"));
                assertFalse(asset.getBody().contains("http://localhost:8080"));
            }
            count++;
        }
        assertTrue(count >= 2);
        String csv = "name,student_id,times,japanese,math,english,deviation_japanese,deviation_math,deviation_english,deviation_three,first_choice,second_choice,third_choice\n"
                + "Distribution Student,1,1,50,50,50,50.1,51.2,52.3,55.7,1,,\n";
        for (String endpoint : new String[]{"csv-import", "csv-preview"}) {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add(endpoint.equals("csv-import") ? "file" : "files", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
                @Override public String getFilename() { return "distribution.csv"; }
            });
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            var result = http.postForEntity("/api/judgements/" + endpoint, new HttpEntity<>(body, headers), JsonNode.class);
            assertEquals(200, result.getStatusCode().value());
            assertEquals("Distribution Student", result.getBody().at("/ledgers/0/studentName").asText());
            assertEquals(5, result.getBody().at("/ledgers/0/results").size());
        }
    }

    @Test void excludesLocalConfigurationAndKeepsSchoolMaster() throws Exception {
        assertFalse(new ClassPathResource("application-local.properties").exists());
        assertFalse(new ClassPathResource("application-local.properties.example").exists());
        String master = new ClassPathResource("school-master.csv").getContentAsString(StandardCharsets.UTF_8);
        assertTrue(master.startsWith("id,name,deviation,school_category"));
    }
}
