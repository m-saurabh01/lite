package com.aircraft.emms.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.nio.file.*;

@Configuration
@EnableAsync
@Slf4j
public class AppConfig {

    @Value("${app.data-dir:./data}")
    private String dataDir;

    @Value("${app.uploads-dir:./data/uploads}")
    private String uploadsDir;

    @PostConstruct
    public void initDirectories() throws IOException {
        createDirIfNotExists(dataDir);
        createDirIfNotExists(uploadsDir);
        createDirIfNotExists(dataDir + "/../logs");
        log.info("Application directories initialized. Data dir: {}", Paths.get(dataDir).toAbsolutePath());
    }

    private void createDirIfNotExists(String dir) throws IOException {
        Path path = Paths.get(dir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Created directory: {}", path.toAbsolutePath());
        }
    }
}
