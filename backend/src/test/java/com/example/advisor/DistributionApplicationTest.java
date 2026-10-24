package com.example.advisor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
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
        assertEquals(200, http.getForEntity("/api/schools", String.class).getStatusCode().value());
    }

    @Test void excludesLocalConfigurationAndKeepsSchoolMaster() throws Exception {
        assertFalse(new ClassPathResource("application-local.properties").exists());
        assertFalse(new ClassPathResource("application-local.properties.example").exists());
        String master = new ClassPathResource("school-master.csv").getContentAsString(StandardCharsets.UTF_8);
        assertTrue(master.startsWith("id,name,deviation,school_category"));
    }
}
