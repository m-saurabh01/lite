package com.aircraft.emms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class EmmsBackendApplication {

    public static void main(String[] args) {
        // Ensure data directory exists before Spring context starts,
        // because SQLite/Flyway need it before @PostConstruct runs.
        String dataDir = System.getenv("EMMS_DATA_DIR");
        if (dataDir == null || dataDir.isBlank()) {
            dataDir = "./data";
        }
        try {
            Path dataPath = Paths.get(dataDir);
            Files.createDirectories(dataPath);
            Files.createDirectories(dataPath.resolve("uploads/xml"));
            Files.createDirectories(Paths.get(dataDir + "/../logs"));
        } catch (Exception e) {
            System.err.println("WARNING: Could not create data directories: " + e.getMessage());
        }

        SpringApplication.run(EmmsBackendApplication.class, args);
    }
}
