package com.slate.locations.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slate.locations.import")
public record LocationCsvImportProperties(
        boolean enabled,
        boolean dryRun,
        boolean exitAfterRun,
        String path,
        String encoding,
        int chunkSize
) {

    public String resolvedPath() {
        return path == null || path.isBlank()
                ? "../assets/영화 로케이션 촬영 이력.csv"
                : path.trim();
    }

    public String resolvedEncoding() {
        return encoding == null || encoding.isBlank() ? "MS949" : encoding.trim();
    }

    public int resolvedChunkSize() {
        return chunkSize > 0 ? chunkSize : 500;
    }
}
