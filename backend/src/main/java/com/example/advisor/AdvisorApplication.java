package com.example.advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdvisorApplication {

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--advisor.desktop=true")) {
            com.example.advisor.desktop.DesktopLauncher.launch();
            return;
        }
        SpringApplication.run(AdvisorApplication.class, args);
    }
}
