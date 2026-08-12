package com.slate.contests;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.slate.common.SlateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContestKoreaCrawlerService {

    private static final String STAGE_DETAIL_FETCH = "DETAIL_FETCH";
    private static final String STAGE_DETAIL_PARSE = "DETAIL_PARSE";
    private static final String STAGE_CONTENT_FILTER = "CONTENT_FILTER";
    private static final String STAGE_NORMALIZE = "NORMALIZE";
    private static final String STAGE_UPSERT = "UPSERT";
    private static final int MESSAGE_LIMIT = 240;
    private static final List<String> PHOTO_ONLY_KEYWORDS = List.of(
            "사진", "사진전", "사진공모", "포토", "스냅", "이미지", "촬영", "photo", "photography", "picture", "snapshot"
    );
    private static final List<String> VIDEO_FILM_KEYWORDS = List.of(
            "영상", "동영상", "비디오", "영화", "영화제", "단편", "장편", "숏폼", "숏츠", "릴스", "유튜브", "브이로그",
            "ucc", "시나리오", "극본", "애니메이션", "다큐", "모션그래픽", "미디어아트", "뮤직비디오",
            "video", "film", "movie", "cinema", "shorts", "reels", "animation", "documentary"
    );

    private final ContestKoreaProperties properties;
    private final ContestKoreaClient client;
    private final ContestKoreaParser parser;
    private final ContestKoreaDataNormalizer normalizer;
    private final ContestKoreaUpsertService upsertService;
    private final Clock clock;

    @Autowired
    public ContestKoreaCrawlerService(
            ContestKoreaProperties properties,
            ContestKoreaClient client,
            ContestKoreaParser parser,
            ContestKoreaDataNormalizer normalizer,
            ContestKoreaUpsertService upsertService
    ) {
        this(properties, client, parser, normalizer, upsertService, Clock.systemDefaultZone());
    }

    ContestKoreaCrawlerService(
            ContestKoreaProperties properties,
            ContestKoreaClient client,
            ContestKoreaParser parser,
            ContestKoreaDataNormalizer normalizer,
            ContestKoreaUpsertService upsertService,
            Clock clock
    ) {
        this.properties = properties;
        this.client = client;
        this.parser = parser;
        this.normalizer = normalizer;
        this.upsertService = upsertService;
        this.clock = clock;
    }

    public ContestKoreaCrawlerRunResult run() {
        return run(null);
    }

    public ContestKoreaCrawlerRunResult run(ContestKoreaCrawlerRunRequest request) {
        requireEnabled();
        int maxPages = clamp(request == null ? null : request.maxPages(), properties.maxPages());
        int maxItems = clamp(request == null ? null : request.maxItems(), properties.maxItemsPerRun());
        boolean dryRun = request != null && Boolean.TRUE.equals(request.dryRun());
        LocalDateTime startedAt = LocalDateTime.now(clock);

        int fetchedPages = 0;
        int discoveredItems = 0;
        Map<String, ContestKoreaListItem> uniqueItems = new LinkedHashMap<>();
        for (int page = 1; page <= maxPages; page++) {
            String listHtml = fetchListPage(page);
            fetchedPages++;
            List<ContestKoreaListItem> pageItems = parseListPage(listHtml, page);
            discoveredItems += pageItems.size();
            for (ContestKoreaListItem item : pageItems) {
                if (StringUtils.hasText(item.sourceExternalId())) {
                    uniqueItems.putIfAbsent(item.sourceExternalId(), item);
                }
            }
        }

        List<ContestKoreaCrawlerItemResult> itemResults = new ArrayList<>();
        uniqueItems.values().stream()
                .limit(maxItems)
                .forEach(item -> itemResults.add(processItem(item, dryRun)));

        int insertedCount = count(itemResults, ContestKoreaCrawlerItemResult.STATUS_INSERTED);
        int updatedCount = count(itemResults, ContestKoreaCrawlerItemResult.STATUS_UPDATED);
        int failedCount = count(itemResults, ContestKoreaCrawlerItemResult.STATUS_FAILED);
        int skippedCount = Math.max(0, uniqueItems.size() - itemResults.size())
                + count(itemResults, ContestKoreaCrawlerItemResult.STATUS_SKIPPED);
        int posterStoredCount = (int) itemResults.stream()
                .filter(ContestKoreaCrawlerItemResult::posterStored)
                .count();

        return new ContestKoreaCrawlerRunResult(
                true,
                dryRun,
                maxPages,
                maxItems,
                fetchedPages,
                discoveredItems,
                uniqueItems.size(),
                itemResults.size(),
                insertedCount,
                updatedCount,
                skippedCount,
                failedCount,
                posterStoredCount,
                startedAt,
                LocalDateTime.now(clock),
                itemResults
        );
    }

    private ContestKoreaCrawlerItemResult processItem(ContestKoreaListItem item, boolean dryRun) {
        String detailHtml;
        try {
            detailHtml = client.fetchDetailPage(item.detailUrl());
        } catch (RuntimeException ex) {
            return ContestKoreaCrawlerItemResult.failed(item, STAGE_DETAIL_FETCH, safeMessage(ex));
        }

        ContestKoreaParsedContest parsed;
        try {
            parsed = parser.parseDetail(detailHtml, item.detailUrl());
        } catch (RuntimeException ex) {
            return ContestKoreaCrawlerItemResult.failed(item, STAGE_DETAIL_PARSE, safeMessage(ex));
        }

        if (isPhotoOnlyContest(parsed)) {
            return ContestKoreaCrawlerItemResult.skipped(item, STAGE_CONTENT_FILTER, "사진 단일 주제 공모전은 수집 대상에서 제외했습니다.");
        }

        ContestKoreaNormalizedContest normalized;
        try {
            normalized = normalizer.normalize(parsed);
        } catch (RuntimeException ex) {
            return ContestKoreaCrawlerItemResult.failed(item, STAGE_NORMALIZE, safeMessage(ex));
        }

        if (dryRun) {
            return ContestKoreaCrawlerItemResult.dryRun(item, normalized);
        }

        try {
            return ContestKoreaCrawlerItemResult.saved(item, upsertService.upsert(normalized));
        } catch (RuntimeException ex) {
            return ContestKoreaCrawlerItemResult.failed(item, STAGE_UPSERT, safeMessage(ex));
        }
    }

    private String fetchListPage(int page) {
        try {
            return client.fetchListPage(page);
        } catch (SlateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 목록 페이지 조회 중 오류가 발생했습니다. page=" + page);
        }
    }

    private List<ContestKoreaListItem> parseListPage(String listHtml, int page) {
        try {
            return parser.parseList(listHtml);
        } catch (SlateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 목록 페이지 파싱 중 오류가 발생했습니다. page=" + page);
        }
    }

    private void requireEnabled() {
        if (!Boolean.TRUE.equals(properties.enabled())) {
            throw new SlateException("콘테스트코리아 크롤러가 비활성화되어 있습니다.");
        }
    }

    private int clamp(Integer requested, int configuredMaximum) {
        int value = requested == null ? configuredMaximum : requested;
        return Math.max(1, Math.min(value, configuredMaximum));
    }

    private int count(List<ContestKoreaCrawlerItemResult> results, String status) {
        return (int) results.stream()
                .filter(result -> status.equals(result.status()))
                .count();
    }

    private boolean isPhotoOnlyContest(ContestKoreaParsedContest parsed) {
        String evidenceText = topicEvidenceText(parsed);
        if (!StringUtils.hasText(evidenceText)) {
            return false;
        }
        String normalized = compact(evidenceText);
        return containsAny(normalized, PHOTO_ONLY_KEYWORDS) && !containsAny(normalized, VIDEO_FILM_KEYWORDS);
    }

    private String topicEvidenceText(ContestKoreaParsedContest parsed) {
        return String.join(" ",
                Objects.toString(parsed.title(), ""),
                Objects.toString(parsed.summary(), ""),
                Objects.toString(parsed.targetText(), ""),
                Objects.toString(parsed.requiredRolesText(), ""),
                Objects.toString(parsed.relatedGenresText(), "")
        );
    }

    private boolean containsAny(String normalized, List<String> keywords) {
        return keywords.stream()
                .map(this::compact)
                .anyMatch(normalized::contains);
    }

    private String compact(String value) {
        return Objects.toString(value, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}·•]+", "");
    }

    private String safeMessage(RuntimeException ex) {
        String message = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MESSAGE_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, MESSAGE_LIMIT);
    }
}
