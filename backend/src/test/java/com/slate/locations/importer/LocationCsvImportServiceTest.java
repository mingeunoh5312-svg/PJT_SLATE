package com.slate.locations.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.locations.importer.LocationCsvImportService.ImportLocation;
import com.slate.locations.importer.LocationCsvImportService.PreparedImport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocationCsvImportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void parsesMs949QuotedCommaAndPreservesRawRowJson() throws Exception {
        Path csv = writeCsv(List.of(
                row("EV-1", "LOC-1", "서울 골목", "테스트 영화", "37.5000000", "127.0000000", "\"쉼표, 포함 장면\"")
        ));
        LocationCsvImportService service = service();

        PreparedImport prepared = service.analyze(csv, Charset.forName("MS949"));

        assertThat(prepared.summary().csvRows()).isEqualTo(1);
        assertThat(prepared.summary().validRows()).isEqualTo(1);
        ImportLocation location = prepared.locations().get(0);
        assertThat(location.values())
                .containsEntry("sourceLocationId", "LOC-1")
                .containsEntry("sourceVariantNo", 1)
                .containsEntry("sourceConflictYn", "N");
        String rawRowJson = (String) location.histories().get(0).values().get("rawRowJson");
        JsonNode raw = objectMapper.readTree(rawRowJson);
        assertThat(raw.path("장면설명").asText()).isEqualTo("쉼표, 포함 장면");
    }

    @Test
    void assignsCoordinateVariantsDeterministicallyAndFlagsAllConflicts() throws Exception {
        Path csv = writeCsv(List.of(
                row("EV-2", "LOC-1", "서울 골목 B", "영화 B", "37.6000000", "127.1000000", "장면 B"),
                row("EV-1", "LOC-1", "서울 골목 A", "영화 A", "37.5000000", "127.0000000", "장면 A")
        ));
        LocationCsvImportService service = service();

        PreparedImport first = service.analyze(csv, Charset.forName("MS949"));
        PreparedImport second = service.analyze(csv, Charset.forName("MS949"));

        assertThat(first.summary().coordinateConflictSourceIds()).isEqualTo(1);
        assertThat(first.summary().locationVariants()).isEqualTo(2);
        assertThat(first.locations()).extracting(location -> location.values().get("sourceVariantNo"))
                .containsExactly(1, 2);
        assertThat(first.locations()).extracting(location -> location.values().get("sourceConflictYn"))
                .containsOnly("Y");
        assertThat(first.locations()).extracting(location -> location.values().get("latitude"))
                .containsExactly(
                        new java.math.BigDecimal("37.5000000"),
                        new java.math.BigDecimal("37.6000000")
                );
        assertThat(second.locations().stream().map(ImportLocation::values).toList())
                .isEqualTo(first.locations().stream().map(ImportLocation::values).toList());
    }

    @Test
    void sourceCsvDryRunMatchesExpectedCounts() {
        LocationCsvImportService service = service();
        Path source = Path.of("../assets/영화 로케이션 촬영 이력.csv").toAbsolutePath().normalize();

        PreparedImport prepared = service.analyze(source, Charset.forName("MS949"));

        assertThat(prepared.summary().expectedCountsMatch()).isTrue();
        assertThat(prepared.summary().errorRows()).isZero();
        assertThat(prepared.summary().requiredMissingCount()).isZero();
        assertThat(prepared.summary().coordinateErrorCount()).isZero();
        assertThat(prepared.locations()).hasSize(9_831);
        assertThat(prepared.locations().stream().mapToInt(location -> location.histories().size()).sum())
                .isEqualTo(13_761);
    }

    private LocationCsvImportService service() {
        return new LocationCsvImportService(
                org.mockito.Mockito.mock(LocationCsvImportWriter.class),
                objectMapper
        );
    }

    private Path writeCsv(List<String> rows) throws Exception {
        String header = String.join(",", LocationCsvImportService.REQUIRED_HEADERS);
        String content = header + "\n" + String.join("\n", rows) + "\n";
        Path csv = tempDir.resolve("locations.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private String row(
            String eventId,
            String locationId,
            String placeName,
            String movieTitle,
            String latitude,
            String longitude,
            String sceneDescription
    ) {
        return String.join(",",
                eventId,
                locationId,
                placeName,
                movieTitle,
                "2025",
                "MOVIE-1",
                sceneDescription,
                "",
                "KMDb",
                "https://example.test/movie",
                "서울특별시",
                "중구",
                "서울특별시 중구 테스트로 1",
                "",
                latitude,
                longitude
        );
    }
}
