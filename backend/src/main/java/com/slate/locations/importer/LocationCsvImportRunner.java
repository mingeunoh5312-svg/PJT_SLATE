package com.slate.locations.importer;

import java.nio.charset.Charset;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.locations.importer.LocationCsvImportService.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "slate.locations.import", name = "enabled", havingValue = "true")
public class LocationCsvImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocationCsvImportRunner.class);

    private final LocationCsvImportService importService;
    private final LocationCsvImportProperties properties;
    private final ObjectMapper objectMapper;
    private final ConfigurableApplicationContext applicationContext;

    public LocationCsvImportRunner(
            LocationCsvImportService importService,
            LocationCsvImportProperties properties,
            ObjectMapper objectMapper,
            ConfigurableApplicationContext applicationContext
    ) {
        this.importService = importService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(properties.resolvedPath()).toAbsolutePath().normalize();
        Charset charset = Charset.forName(properties.resolvedEncoding());
        ImportResult result = importService.run(
                path,
                charset,
                properties.dryRun(),
                properties.resolvedChunkSize()
        );
        log.info("LOCATION_CSV_IMPORT_RESULT {}", objectMapper.writeValueAsString(result));
        if (properties.exitAfterRun()) {
            SpringApplication.exit(applicationContext);
        }
    }
}
