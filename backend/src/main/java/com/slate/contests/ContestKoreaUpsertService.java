package com.slate.contests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.slate.common.SlateException;
import com.slate.media.MediaImageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContestKoreaUpsertService {

    private static final String POSTER_ALLOWED = "CONTESTKOREA_ALLOWED";
    private static final String POSTER_NONE = "NONE";
    private static final DateTimeFormatter ROW_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> SYNC_COMPARE_KEYS = List.of(
            "contestType",
            "title",
            "summary",
            "theme",
            "prizeText",
            "totalPrizeAmount",
            "firstPrizeAmount",
            "organizer",
            "organizerType",
            "representativeImageUrl",
            "representativeImagePath",
            "posterSourceType",
            "posterOriginalUrl",
            "externalUrl",
            "targetText",
            "targetCodesJson",
            "regionCodesJson",
            "requiredRolesText",
            "relatedGenresText",
            "startAt",
            "deadlineAt",
            "status",
            "sourceUrl",
            "sourceCategoryCode",
            "sourcePermissionText",
            "sourceAttribution"
    );
    private static final List<String> POSTER_COMPARE_KEYS = List.of(
            "representativeImagePath",
            "posterSourceType",
            "posterOriginalUrl"
    );

    private final ContestMapper contestMapper;
    private final ContestKoreaPosterStorageService posterStorageService;
    private final MediaImageService mediaImageService;

    public ContestKoreaUpsertService(
            ContestMapper contestMapper,
            ContestKoreaPosterStorageService posterStorageService,
            MediaImageService mediaImageService
    ) {
        this.contestMapper = contestMapper;
        this.posterStorageService = posterStorageService;
        this.mediaImageService = mediaImageService;
    }

    @Transactional
    public ContestKoreaUpsertResult upsert(ContestKoreaNormalizedContest normalized) {
        requireNormalized(normalized);
        String sourceName = requireText(normalized.sourceName(), "sourceName");
        String sourceExternalId = requireText(normalized.sourceExternalId(), "sourceExternalId");
        Map<String, Object> existing = contestMapper.selectContestBySource(sourceName, sourceExternalId);
        Map<String, Object> row = new LinkedHashMap<>(normalized.toRow());
        if (canSkipBeforePosterStorage(row, existing)) {
            return unchangedResult(row, existing, sourceName, sourceExternalId);
        }
        return doUpsert(row, existing, sourceName, sourceExternalId, storePosterForCrawler(normalized));
    }

    @Transactional
    public ContestKoreaUpsertResult upsert(ContestKoreaNormalizedContest normalized, ContestKoreaPosterStorageResult posterResult) {
        requireNormalized(normalized);
        if (posterResult == null) {
            throw new SlateException("콘테스트코리아 포스터 저장 결과가 필요합니다.");
        }

        String sourceName = requireText(normalized.sourceName(), "sourceName");
        String sourceExternalId = requireText(normalized.sourceExternalId(), "sourceExternalId");
        Map<String, Object> existing = contestMapper.selectContestBySource(sourceName, sourceExternalId);
        Map<String, Object> row = new LinkedHashMap<>(normalized.toRow());
        return doUpsert(row, existing, sourceName, sourceExternalId, posterResult);
    }

    private ContestKoreaUpsertResult doUpsert(
            Map<String, Object> row,
            Map<String, Object> existing,
            String sourceName,
            String sourceExternalId,
            ContestKoreaPosterStorageResult posterResult
    ) {
        mergePosterResult(row, existing, posterResult);

        String newPath = textOrNull(row.get("representativeImagePath"));
        if (sameCrawlerPayload(row, existing)) {
            cleanupNewPoster(posterResult, newPath);
            return unchangedResult(row, existing, sourceName, sourceExternalId);
        }

        Long contestId;
        try {
            int affected = contestMapper.upsertContestFromCrawler(row);
            if (affected == 0) {
                throw new SlateException("콘테스트코리아 공모전 저장 결과가 올바르지 않습니다.");
            }
            contestId = contestId(row, existing);
            String oldPath = textOrNull(existing == null ? null : existing.get("representativeImagePath"));
            if (oldPath != null && !Objects.equals(oldPath, newPath)) {
                mediaImageService.deleteStoredAfterCommit(oldPath);
            }
        } catch (RuntimeException ex) {
            cleanupNewPoster(posterResult, newPath);
            throw ex;
        }
        return new ContestKoreaUpsertResult(
                contestId,
                sourceName,
                sourceExternalId,
                existing == null,
                existing != null,
                posterResult.stored(),
                newPath
        );
    }

    private ContestKoreaPosterStorageResult storePosterForCrawler(ContestKoreaNormalizedContest normalized) {
        try {
            return posterStorageService.storePosterIfAllowed(normalized);
        } catch (SlateException ex) {
            if (HttpStatus.INTERNAL_SERVER_ERROR.equals(ex.status())) {
                throw ex;
            }
            return ContestKoreaPosterStorageResult.skipped(normalized);
        }
    }

    private boolean canSkipBeforePosterStorage(Map<String, Object> row, Map<String, Object> existing) {
        if (existing == null || !sameCrawlerContent(row, existing)) {
            return false;
        }
        String posterSourceType = normalizeCode(row.get("posterSourceType"));
        String existingPosterSourceType = normalizeCode(existing.get("posterSourceType"));
        String posterOriginalUrl = textOrNull(row.get("posterOriginalUrl"));
        if (POSTER_ALLOWED.equals(posterSourceType) && StringUtils.hasText(posterOriginalUrl)) {
            return POSTER_ALLOWED.equals(existingPosterSourceType)
                    && Objects.equals(posterOriginalUrl, textOrNull(existing.get("posterOriginalUrl")))
                    && StringUtils.hasText(textOrNull(existing.get("representativeImagePath")));
        }
        Map<String, Object> expected = new LinkedHashMap<>(row);
        mergePosterResult(expected, existing, new ContestKoreaPosterStorageResult(
                false,
                null,
                null,
                null,
                textOrNull(row.get("posterSourceType")),
                textOrNull(row.get("sourcePermissionText")),
                textOrNull(row.get("sourceAttribution"))
        ));
        return sameCrawlerPayload(expected, existing);
    }

    private ContestKoreaUpsertResult unchangedResult(
            Map<String, Object> row,
            Map<String, Object> existing,
            String sourceName,
            String sourceExternalId
    ) {
        return new ContestKoreaUpsertResult(
                contestId(row, existing),
                sourceName,
                sourceExternalId,
                false,
                false,
                false,
                textOrNull(firstObject(row.get("representativeImagePath"), existing.get("representativeImagePath")))
        );
    }

    private boolean sameCrawlerContent(Map<String, Object> row, Map<String, Object> existing) {
        return SYNC_COMPARE_KEYS.stream()
                .filter(key -> !POSTER_COMPARE_KEYS.contains(key))
                .allMatch(key -> sameValue(row.get(key), existing.get(key)));
    }

    private boolean sameCrawlerPayload(Map<String, Object> row, Map<String, Object> existing) {
        return existing != null && SYNC_COMPARE_KEYS.stream()
                .allMatch(key -> sameValue(row.get(key), existing.get(key)));
    }

    private boolean sameValue(Object first, Object second) {
        return Objects.equals(compareValue(first), compareValue(second));
    }

    private Object compareValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(ROW_DATE_TIME);
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
        }
        String text = textOrNull(value);
        if (text == null) {
            return null;
        }
        return text.replace('T', ' ');
    }

    private void mergePosterResult(
            Map<String, Object> row,
            Map<String, Object> existing,
            ContestKoreaPosterStorageResult posterResult
    ) {
        if (posterResult.stored()) {
            row.put("representativeImagePath", requireText(posterResult.representativeImagePath(), "representativeImagePath"));
            row.put("posterOriginalUrl", posterResult.posterOriginalUrl());
            row.put("posterCollectedAt", posterResult.posterCollectedAt());
            row.put("posterSourceType", POSTER_ALLOWED);
            row.put("sourcePermissionText", posterResult.sourcePermissionText());
            row.put("sourceAttribution", posterResult.sourceAttribution());
            return;
        }

        if (posterPermissionCleared(posterResult)) {
            row.put("representativeImagePath", null);
            row.put("posterOriginalUrl", null);
            row.put("posterCollectedAt", null);
            row.put("posterSourceType", POSTER_NONE);
            row.put("sourcePermissionText", null);
            if (StringUtils.hasText(posterResult.sourceAttribution())) {
                row.put("sourceAttribution", posterResult.sourceAttribution());
            }
            return;
        }

        row.put("representativeImagePath", textOrNull(existing == null ? null : existing.get("representativeImagePath")));
        row.put("posterOriginalUrl", firstText(posterResult.posterOriginalUrl(), existing == null ? null : existing.get("posterOriginalUrl")));
        row.put("posterCollectedAt", firstObject(posterResult.posterCollectedAt(), existing == null ? null : existing.get("posterCollectedAt")));
        row.put("posterSourceType", firstText(posterResult.posterSourceType(), existing == null ? null : existing.get("posterSourceType")));
        row.put("sourcePermissionText", firstText(posterResult.sourcePermissionText(), row.get("sourcePermissionText")));
        row.put("sourceAttribution", firstText(posterResult.sourceAttribution(), row.get("sourceAttribution")));
    }

    private boolean posterPermissionCleared(ContestKoreaPosterStorageResult posterResult) {
        return !POSTER_ALLOWED.equals(normalizeCode(posterResult.posterSourceType()))
                || !StringUtils.hasText(posterResult.sourcePermissionText());
    }

    private void cleanupNewPoster(ContestKoreaPosterStorageResult posterResult, String newPath) {
        if (posterResult.stored() && StringUtils.hasText(newPath)) {
            posterStorageService.deleteStoredImmediately(newPath);
        }
    }

    private void requireNormalized(ContestKoreaNormalizedContest normalized) {
        if (normalized == null) {
            throw new SlateException("콘테스트코리아 정규화 결과가 필요합니다.");
        }
    }

    private String requireText(String value, String label) {
        String normalized = textOrNull(value);
        if (normalized == null) {
            throw new SlateException(label + "은 필수입니다.");
        }
        return normalized;
    }

    private Long contestId(Map<String, Object> row, Map<String, Object> existing) {
        Long id = longValue(row.get("contestId"));
        if (id == null && existing != null) {
            id = longValue(existing.get("contestId"));
        }
        if (id == null) {
            throw new SlateException("콘테스트코리아 공모전 ID를 확인하지 못했습니다.");
        }
        return id;
    }

    private Object firstObject(Object first, Object fallback) {
        return first != null ? first : fallback;
    }

    private String firstText(Object first, Object fallback) {
        String value = textOrNull(first);
        return value == null ? textOrNull(fallback) : value;
    }

    private String textOrNull(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private String normalizeCode(Object value) {
        String normalized = textOrNull(value);
        return normalized == null ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = textOrNull(value);
        return text == null ? null : Long.valueOf(text);
    }
}
