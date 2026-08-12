package com.slate.locations.importer;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import com.slate.locations.importer.LocationCsvImportWriter.WriteCount;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocationCsvImportService {

    static final int EXPECTED_CSV_ROWS = 13_761;
    static final int EXPECTED_SOURCE_LOCATION_IDS = 9_811;
    static final int EXPECTED_CONFLICT_SOURCE_IDS = 20;
    static final int EXPECTED_LOCATION_VARIANTS = 9_831;
    static final int EXPECTED_EVENT_IDS = 13_761;

    static final List<String> REQUIRED_HEADERS = List.of(
            "사건일련번호",
            "장소일련번호",
            "촬영장소명",
            "작품명",
            "제작연도",
            "영화작품코드",
            "장면설명",
            "등장인물",
            "출처",
            "출처 웹페이지주소(URL)",
            "시도",
            "시군구",
            "지번주소",
            "도로명주소",
            "위도",
            "경도"
    );

    private static final int MAX_REPORTED_ERRORS = 20;
    private static final Comparator<CoordinateKey> COORDINATE_COMPARATOR = Comparator
            .comparing(CoordinateKey::latitude)
            .thenComparing(CoordinateKey::longitude);

    private final LocationCsvImportWriter writer;
    private final ObjectMapper objectMapper;

    public LocationCsvImportService(LocationCsvImportWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.objectMapper = objectMapper;
    }

    public ImportResult run(Path path, Charset charset, boolean dryRun, int chunkSize) {
        PreparedImport prepared = analyze(path, charset);
        if (dryRun) {
            return new ImportResult(
                    true,
                    path.toString(),
                    charset.name(),
                    prepared.summary(),
                    0,
                    0,
                    0
            );
        }

        assertWritable(prepared.summary());
        int safeChunkSize = Math.max(1, chunkSize);
        int writtenLocations = 0;
        int writtenHistories = 0;
        int chunks = 0;
        List<ImportLocation> locations = prepared.locations();
        for (int start = 0; start < locations.size(); start += safeChunkSize) {
            int end = Math.min(locations.size(), start + safeChunkSize);
            WriteCount count = writer.writeChunk(locations.subList(start, end));
            writtenLocations += count.locations();
            writtenHistories += count.histories();
            chunks++;
        }
        return new ImportResult(
                false,
                path.toString(),
                charset.name(),
                prepared.summary(),
                writtenLocations,
                writtenHistories,
                chunks
        );
    }

    PreparedImport analyze(Path path, Charset charset) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new SlateException("로케이션 CSV 파일을 찾을 수 없습니다.");
        }
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false)
                .get();
        AnalysisAccumulator accumulator = new AnalysisAccumulator();
        List<ParsedRow> rows = new ArrayList<>(EXPECTED_CSV_ROWS);

        try (Reader reader = Channels.newReader(
                Files.newByteChannel(path, StandardOpenOption.READ),
                decoder,
                -1
        ); CSVParser parser = CSVParser.parse(reader, format)) {
            validateHeaders(parser);
            for (CSVRecord record : parser) {
                accumulator.csvRows++;
                ParsedRow row = parseRow(record, accumulator);
                if (row != null) {
                    rows.add(row);
                }
            }
        } catch (SlateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SlateException("로케이션 CSV를 읽지 못했습니다: " + safeMessage(ex));
        }

        Set<String> eventIds = new LinkedHashSet<>();
        int duplicateEventIds = 0;
        for (ParsedRow row : rows) {
            if (!eventIds.add(row.sourceEventId())) {
                duplicateEventIds++;
                accumulator.addError(row.rowNumber(), "사건일련번호가 중복되었습니다.");
            }
        }

        Map<String, TreeMap<CoordinateKey, List<ParsedRow>>> grouped = new TreeMap<>();
        for (ParsedRow row : rows) {
            grouped.computeIfAbsent(row.sourceLocationId(), ignored -> new TreeMap<>(COORDINATE_COMPARATOR))
                    .computeIfAbsent(row.coordinate(), ignored -> new ArrayList<>())
                    .add(row);
        }

        int conflictSourceIds = 0;
        int locationVariants = 0;
        List<ImportLocation> locations = new ArrayList<>();
        for (Map.Entry<String, TreeMap<CoordinateKey, List<ParsedRow>>> sourceEntry : grouped.entrySet()) {
            TreeMap<CoordinateKey, List<ParsedRow>> variants = sourceEntry.getValue();
            boolean conflict = variants.size() > 1;
            if (conflict) {
                conflictSourceIds++;
            }
            int variantNo = 1;
            for (Map.Entry<CoordinateKey, List<ParsedRow>> variantEntry : variants.entrySet()) {
                List<ParsedRow> variantRows = variantEntry.getValue().stream()
                        .sorted(Comparator.comparing(ParsedRow::sourceEventId).thenComparingLong(ParsedRow::rowNumber))
                        .toList();
                locations.add(toImportLocation(
                        sourceEntry.getKey(),
                        variantNo++,
                        conflict,
                        variantEntry.getKey(),
                        variantRows
                ));
                locationVariants++;
            }
        }

        ImportSummary summary = new ImportSummary(
                accumulator.csvRows,
                rows.size(),
                accumulator.errorRows,
                grouped.size(),
                conflictSourceIds,
                locationVariants,
                eventIds.size(),
                duplicateEventIds,
                accumulator.requiredMissingCount,
                accumulator.coordinateErrorCount,
                List.copyOf(accumulator.errors)
        );
        return new PreparedImport(List.copyOf(locations), summary);
    }

    private ParsedRow parseRow(CSVRecord record, AnalysisAccumulator accumulator) {
        long rowNumber = record.getRecordNumber() + 1;
        Map<String, String> raw = new LinkedHashMap<>();
        for (String header : REQUIRED_HEADERS) {
            raw.put(header, clean(record.get(header)));
        }

        List<String> reasons = new ArrayList<>();
        String sourceEventId = required(raw, "사건일련번호", reasons, accumulator);
        String sourceLocationId = required(raw, "장소일련번호", reasons, accumulator);
        String placeName = required(raw, "촬영장소명", reasons, accumulator);
        String movieTitle = required(raw, "작품명", reasons, accumulator);
        String movieCode = required(raw, "영화작품코드", reasons, accumulator);
        String sourceName = required(raw, "출처", reasons, accumulator);
        String sido = required(raw, "시도", reasons, accumulator);
        BigDecimal latitude = coordinate(raw.get("위도"), true, reasons, accumulator);
        BigDecimal longitude = coordinate(raw.get("경도"), false, reasons, accumulator);
        Integer productionYear = productionYear(raw.get("제작연도"), reasons);

        validateLength(sourceEventId, 20, "사건일련번호", reasons);
        validateLength(sourceLocationId, 20, "장소일련번호", reasons);
        validateLength(placeName, 120, "촬영장소명", reasons);
        validateLength(movieCode, 20, "영화작품코드", reasons);
        validateLength(movieTitle, 120, "작품명", reasons);
        validateLength(sourceName, 100, "출처", reasons);
        validateLength(raw.get("시군구"), 80, "시군구", reasons);
        validateLength(raw.get("지번주소"), 255, "지번주소", reasons);
        validateLength(raw.get("도로명주소"), 255, "도로명주소", reasons);
        validateLength(raw.get("장면설명"), 1000, "장면설명", reasons);
        validateLength(raw.get("등장인물"), 300, "등장인물", reasons);
        validateLength(raw.get("출처 웹페이지주소(URL)"), 1000, "출처 URL", reasons);

        if (!reasons.isEmpty()) {
            accumulator.errorRows++;
            accumulator.addError(rowNumber, String.join("; ", reasons));
            return null;
        }

        return new ParsedRow(
                rowNumber,
                sourceEventId,
                sourceLocationId,
                placeName,
                movieTitle,
                productionYear,
                movieCode,
                nullable(raw.get("장면설명")),
                nullable(raw.get("등장인물")),
                sourceName,
                nullable(raw.get("출처 웹페이지주소(URL)")),
                sido,
                nullable(raw.get("시군구")),
                nullable(raw.get("지번주소")),
                nullable(raw.get("도로명주소")),
                new CoordinateKey(latitude, longitude),
                raw
        );
    }

    private ImportLocation toImportLocation(
            String sourceLocationId,
            int variantNo,
            boolean conflict,
            CoordinateKey coordinate,
            List<ParsedRow> rows
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceLocationId", sourceLocationId);
        values.put("sourceVariantNo", variantNo);
        values.put("placeName", firstValue(rows, ParsedRow::placeName));
        values.put("sido", firstValue(rows, ParsedRow::sido));
        values.put("sigungu", firstValue(rows, ParsedRow::sigungu));
        values.put("lotAddress", firstValue(rows, ParsedRow::lotAddress));
        values.put("roadAddress", firstValue(rows, ParsedRow::roadAddress));
        values.put("latitude", coordinate.latitude());
        values.put("longitude", coordinate.longitude());
        values.put("sourceConflictYn", conflict ? "Y" : "N");
        values.put("qualityFlagsJson", qualityFlags(rows));
        values.put("searchText", searchText(rows));

        List<ImportHistory> histories = rows.stream()
                .map(row -> new ImportHistory(historyValues(row)))
                .toList();
        return new ImportLocation(values, histories);
    }

    private Map<String, Object> historyValues(ParsedRow row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceEventId", row.sourceEventId());
        values.put("movieCode", row.movieCode());
        values.put("movieTitle", row.movieTitle());
        values.put("productionYear", row.productionYear());
        values.put("sceneDescription", row.sceneDescription());
        values.put("characters", row.characters());
        values.put("sourceName", row.sourceName());
        values.put("sourceUrl", row.sourceUrl());
        values.put("rawRowJson", toJson(row.raw()));
        return values;
    }

    private String qualityFlags(List<ParsedRow> rows) {
        List<String> conflicts = new ArrayList<>();
        addConflict(conflicts, "placeName", rows, ParsedRow::placeName);
        addConflict(conflicts, "sido", rows, ParsedRow::sido);
        addConflict(conflicts, "sigungu", rows, ParsedRow::sigungu);
        addConflict(conflicts, "lotAddress", rows, ParsedRow::lotAddress);
        addConflict(conflicts, "roadAddress", rows, ParsedRow::roadAddress);
        Map<String, Object> flags = new LinkedHashMap<>();
        if (!conflicts.isEmpty()) {
            flags.put("fieldConflicts", conflicts);
        }
        if (rows.stream().allMatch(row -> !StringUtils.hasText(row.lotAddress()))) {
            flags.put("missingLotAddress", true);
        }
        if (rows.stream().allMatch(row -> !StringUtils.hasText(row.roadAddress()))) {
            flags.put("missingRoadAddress", true);
        }
        return flags.isEmpty() ? null : toJson(flags);
    }

    private String searchText(List<ParsedRow> rows) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (ParsedRow row : rows) {
            addText(values, row.placeName());
            addText(values, row.sido());
            addText(values, row.sigungu());
            addText(values, row.lotAddress());
            addText(values, row.roadAddress());
            addText(values, row.movieTitle());
            addText(values, row.sceneDescription());
        }
        return String.join(" ", values);
    }

    private void validateHeaders(CSVParser parser) {
        List<String> actual = new ArrayList<>(parser.getHeaderMap().keySet());
        if (!actual.equals(REQUIRED_HEADERS)) {
            throw new SlateException("로케이션 CSV 헤더가 예상 형식과 일치하지 않습니다.");
        }
    }

    private void assertWritable(ImportSummary summary) {
        if (summary.errorRows() != 0
                || summary.duplicateEventIds() != 0
                || !summary.expectedCountsMatch()) {
            throw new SlateException(
                    "로케이션 CSV 핵심 수치 또는 유효성 검증이 실패하여 write를 중단했습니다. "
                            + "rows=" + summary.csvRows()
                            + ", valid=" + summary.validRows()
                            + ", errors=" + summary.errorRows()
                            + ", sourceIds=" + summary.sourceLocationIds()
                            + ", conflicts=" + summary.coordinateConflictSourceIds()
                            + ", variants=" + summary.locationVariants()
                            + ", events=" + summary.distinctEventIds()
                            + ", duplicateEvents=" + summary.duplicateEventIds()
            );
        }
    }

    private String required(
            Map<String, String> raw,
            String header,
            List<String> reasons,
            AnalysisAccumulator accumulator
    ) {
        String value = raw.get(header);
        if (!StringUtils.hasText(value)) {
            reasons.add(header + " 필수값이 없습니다.");
            accumulator.requiredMissingCount++;
            return null;
        }
        return value;
    }

    private BigDecimal coordinate(
            String value,
            boolean latitude,
            List<String> reasons,
            AnalysisAccumulator accumulator
    ) {
        String label = latitude ? "위도" : "경도";
        if (!StringUtils.hasText(value)) {
            reasons.add(label + " 값이 없습니다.");
            accumulator.coordinateErrorCount++;
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim()).setScale(7, RoundingMode.HALF_UP);
            BigDecimal minimum = BigDecimal.valueOf(latitude ? -90 : -180);
            BigDecimal maximum = BigDecimal.valueOf(latitude ? 90 : 180);
            if (parsed.compareTo(minimum) < 0 || parsed.compareTo(maximum) > 0) {
                reasons.add(label + " 범위를 벗어났습니다.");
                accumulator.coordinateErrorCount++;
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            reasons.add(label + " 형식이 올바르지 않습니다.");
            accumulator.coordinateErrorCount++;
            return null;
        }
    }

    private Integer productionYear(String value, List<String> reasons) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            int year = Integer.parseInt(value.trim());
            if (year < 1800 || year > 2200) {
                reasons.add("제작연도 범위를 벗어났습니다.");
                return null;
            }
            return year;
        } catch (NumberFormatException ex) {
            reasons.add("제작연도 형식이 올바르지 않습니다.");
            return null;
        }
    }

    private void validateLength(String value, int maximum, String label, List<String> reasons) {
        if (value != null && value.length() > maximum) {
            reasons.add(label + " 길이가 " + maximum + "자를 초과합니다.");
        }
    }

    private <T> String firstValue(List<T> rows, Function<T, String> extractor) {
        return rows.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private <T> void addConflict(
            List<String> conflicts,
            String field,
            List<T> rows,
            Function<T, String> extractor
    ) {
        long count = rows.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(2)
                .count();
        if (count > 1) {
            conflicts.add(field);
        }
    }

    private void addText(Set<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    private String clean(String value) {
        return Objects.toString(value, "").trim();
    }

    private String nullable(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new SlateException("로케이션 CSV JSON 변환에 실패했습니다.");
        }
    }

    private String safeMessage(Exception ex) {
        String message = Objects.toString(ex.getMessage(), ex.getClass().getSimpleName());
        return message.length() <= 300 ? message : message.substring(0, 300);
    }

    record CoordinateKey(BigDecimal latitude, BigDecimal longitude) {
    }

    record ParsedRow(
            long rowNumber,
            String sourceEventId,
            String sourceLocationId,
            String placeName,
            String movieTitle,
            Integer productionYear,
            String movieCode,
            String sceneDescription,
            String characters,
            String sourceName,
            String sourceUrl,
            String sido,
            String sigungu,
            String lotAddress,
            String roadAddress,
            CoordinateKey coordinate,
            Map<String, String> raw
    ) {
    }

    public record ImportLocation(Map<String, Object> values, List<ImportHistory> histories) {
    }

    public record ImportHistory(Map<String, Object> values) {
    }

    record PreparedImport(List<ImportLocation> locations, ImportSummary summary) {
    }

    public record ImportSummary(
            int csvRows,
            int validRows,
            int errorRows,
            int sourceLocationIds,
            int coordinateConflictSourceIds,
            int locationVariants,
            int distinctEventIds,
            int duplicateEventIds,
            int requiredMissingCount,
            int coordinateErrorCount,
            List<ImportError> errors
    ) {

        public boolean expectedCountsMatch() {
            return csvRows == EXPECTED_CSV_ROWS
                    && validRows == EXPECTED_CSV_ROWS
                    && sourceLocationIds == EXPECTED_SOURCE_LOCATION_IDS
                    && coordinateConflictSourceIds == EXPECTED_CONFLICT_SOURCE_IDS
                    && locationVariants == EXPECTED_LOCATION_VARIANTS
                    && distinctEventIds == EXPECTED_EVENT_IDS
                    && duplicateEventIds == 0;
        }
    }

    public record ImportError(long rowNumber, String reason) {
    }

    public record ImportResult(
            boolean dryRun,
            String inputPath,
            String encoding,
            ImportSummary summary,
            int writtenLocations,
            int writtenHistories,
            int chunks
    ) {
    }

    private static final class AnalysisAccumulator {

        private int csvRows;
        private int errorRows;
        private int requiredMissingCount;
        private int coordinateErrorCount;
        private final List<ImportError> errors = new ArrayList<>();

        private void addError(long rowNumber, String reason) {
            if (errors.size() < MAX_REPORTED_ERRORS) {
                errors.add(new ImportError(rowNumber, reason));
            }
        }
    }
}
