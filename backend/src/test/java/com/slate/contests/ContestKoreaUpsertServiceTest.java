package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.media.MediaImageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContestKoreaUpsertServiceTest {

    private static final String POSTER_URL = "https://www.contestkorea.com/upload/contest/poster-2026.jpg";
    private static final LocalDateTime POSTER_COLLECTED_AT = LocalDateTime.of(2026, 6, 23, 11, 5);

    @Test
    void upsertMergesNormalizedRowAndStoredPosterResultBeforeMapperCall() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(null);
        when(fixture.mapper.upsertContestFromCrawler(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("contestId", 41L);
            return 1;
        });

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized(), storedPoster("images/contest/2026/06/new.jpg"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.mapper).upsertContestFromCrawler(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("contestType", "EXTERNAL")
                .containsEntry("sourceName", "CONTESTKOREA")
                .containsEntry("sourceExternalId", "202606170004")
                .containsEntry("representativeImagePath", "images/contest/2026/06/new.jpg")
                .containsEntry("posterSourceType", "CONTESTKOREA_ALLOWED")
                .containsEntry("posterOriginalUrl", POSTER_URL)
                .containsEntry("posterCollectedAt", POSTER_COLLECTED_AT)
                .containsEntry("submissionEmail", null);
        assertThat(result)
                .extracting(ContestKoreaUpsertResult::contestId, ContestKoreaUpsertResult::inserted,
                        ContestKoreaUpsertResult::updated, ContestKoreaUpsertResult::posterStored,
                        ContestKoreaUpsertResult::representativeImagePath)
                .containsExactly(41L, true, false, true, "images/contest/2026/06/new.jpg");
    }

    @Test
    void upsertCanCallPosterStorageInternally() {
        Fixture fixture = fixture();
        ContestKoreaNormalizedContest normalized = normalized();
        when(fixture.posterStorage.storePosterIfAllowed(normalized)).thenReturn(storedPoster("images/contest/2026/06/new.jpg"));
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(null);
        when(fixture.mapper.upsertContestFromCrawler(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("contestId", 42L);
            return 1;
        });

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized);

        verify(fixture.posterStorage).storePosterIfAllowed(normalized);
        assertThat(result.contestId()).isEqualTo(42L);
        assertThat(result.posterStored()).isTrue();
    }

    @Test
    void posterStorageValidationFailureSkipsPosterAndStillUpsertsContest() {
        Fixture fixture = fixture();
        ContestKoreaNormalizedContest normalized = normalized();
        when(fixture.posterStorage.storePosterIfAllowed(normalized))
                .thenThrow(new SlateException("콘테스트코리아 포스터 파일 내용이 JPEG, PNG, WebP가 아닙니다."));
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(null);
        when(fixture.mapper.upsertContestFromCrawler(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("contestId", 43L);
            return 1;
        });

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.mapper).upsertContestFromCrawler(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("representativeImagePath", null)
                .containsEntry("posterSourceType", "CONTESTKOREA_ALLOWED")
                .containsEntry("posterOriginalUrl", POSTER_URL);
        assertThat(result)
                .extracting(ContestKoreaUpsertResult::contestId, ContestKoreaUpsertResult::inserted,
                        ContestKoreaUpsertResult::posterStored, ContestKoreaUpsertResult::representativeImagePath)
                .containsExactly(43L, true, false, null);
    }

    @Test
    void unchangedExistingContestSkipsPosterDownloadAndDatabaseUpdate() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004"))
                .thenReturn(existingMatching("images/contest/2026/05/old.jpg"));

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized());

        verify(fixture.posterStorage, never()).storePosterIfAllowed(any());
        verify(fixture.mapper, never()).upsertContestFromCrawler(any());
        verify(fixture.media, never()).deleteStoredAfterCommit(any());
        assertThat(result)
                .extracting(ContestKoreaUpsertResult::contestId, ContestKoreaUpsertResult::inserted,
                        ContestKoreaUpsertResult::updated, ContestKoreaUpsertResult::posterStored,
                        ContestKoreaUpsertResult::representativeImagePath)
                .containsExactly(31L, false, false, false, "images/contest/2026/05/old.jpg");
    }

    @Test
    void missingSourceNameOrExternalIdFailsBeforeMapperMutation() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.upsert(normalized(builder -> builder.sourceName = null), skippedAllowedPoster()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("sourceName");
        assertThatThrownBy(() -> fixture.service.upsert(normalized(builder -> builder.sourceExternalId = " "), skippedAllowedPoster()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("sourceExternalId");

        verify(fixture.mapper, never()).selectContestBySource(any(), any());
        verify(fixture.mapper, never()).upsertContestFromCrawler(any());
    }

    @Test
    void missingSourceOnInternalPosterStoragePathFailsBeforePosterStorage() {
        Fixture fixture = fixture();
        ContestKoreaNormalizedContest missingSource = normalized(builder -> builder.sourceName = null);

        assertThatThrownBy(() -> fixture.service.upsert(missingSource))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("sourceName");

        verify(fixture.posterStorage, never()).storePosterIfAllowed(any());
        verify(fixture.mapper, never()).upsertContestFromCrawler(any());
    }

    @Test
    void permissionClearedPosterResultClearsPosterFieldsAndSchedulesOldFileDelete() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(existing("images/contest/2026/05/old.jpg"));
        when(fixture.mapper.upsertContestFromCrawler(any())).thenReturn(1);

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized(), permissionClearedPoster());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.mapper).upsertContestFromCrawler(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("representativeImagePath", null)
                .containsEntry("posterSourceType", "NONE")
                .containsEntry("posterOriginalUrl", null)
                .containsEntry("posterCollectedAt", null)
                .containsEntry("sourcePermissionText", null);
        verify(fixture.media).deleteStoredAfterCommit("images/contest/2026/05/old.jpg");
        assertThat(result.updated()).isTrue();
        assertThat(result.representativeImagePath()).isNull();
    }

    @Test
    void allowedButSkippedPosterResultPreservesExistingPosterFileAndMetadata() {
        Fixture fixture = fixture();
        Map<String, Object> existing = existing("images/contest/2026/05/old.jpg");
        existing.put("posterOriginalUrl", "https://www.contestkorea.com/upload/contest/old.jpg");
        existing.put("posterCollectedAt", LocalDateTime.of(2026, 5, 20, 10, 0));
        existing.put("posterSourceType", "CONTESTKOREA_ALLOWED");
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(existing);
        when(fixture.mapper.upsertContestFromCrawler(any())).thenReturn(1);

        fixture.service.upsert(normalized(), skippedAllowedPoster());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.mapper).upsertContestFromCrawler(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("representativeImagePath", "images/contest/2026/05/old.jpg")
                .containsEntry("posterSourceType", "CONTESTKOREA_ALLOWED")
                .containsEntry("posterOriginalUrl", "https://www.contestkorea.com/upload/contest/old.jpg")
                .containsEntry("posterCollectedAt", LocalDateTime.of(2026, 5, 20, 10, 0));
        verify(fixture.media, never()).deleteStoredAfterCommit(any());
    }

    @Test
    void existingRowReturnsUpdatedResult() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(existing(null));
        when(fixture.mapper.upsertContestFromCrawler(any())).thenReturn(2);

        ContestKoreaUpsertResult result = fixture.service.upsert(normalized(), skippedAllowedPoster());

        assertThat(result.contestId()).isEqualTo(31L);
        assertThat(result.inserted()).isFalse();
        assertThat(result.updated()).isTrue();
    }

    @Test
    void newPosterReplacesOldPosterAfterCommit() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(existing("images/contest/2026/05/old.jpg"));
        when(fixture.mapper.upsertContestFromCrawler(any())).thenReturn(2);

        fixture.service.upsert(normalized(), storedPoster("images/contest/2026/06/new.jpg"));

        verify(fixture.media).deleteStoredAfterCommit("images/contest/2026/05/old.jpg");
    }

    @Test
    void databaseFailureDeletesNewlyStoredPosterImmediately() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(existing("images/contest/2026/05/old.jpg"));
        when(fixture.mapper.upsertContestFromCrawler(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> fixture.service.upsert(normalized(), storedPoster("images/contest/2026/06/new.jpg")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        verify(fixture.posterStorage).deleteStoredImmediately("images/contest/2026/06/new.jpg");
        verify(fixture.media, never()).deleteStoredAfterCommit(any());
    }

    @Test
    void zeroAffectedRowsFailsAndDeletesNewlyStoredPosterImmediately() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectContestBySource("CONTESTKOREA", "202606170004")).thenReturn(null);
        when(fixture.mapper.upsertContestFromCrawler(any())).thenReturn(0);

        assertThatThrownBy(() -> fixture.service.upsert(normalized(), storedPoster("images/contest/2026/06/new.jpg")))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("저장 결과");

        verify(fixture.posterStorage).deleteStoredImmediately("images/contest/2026/06/new.jpg");
    }

    private Fixture fixture() {
        ContestMapper mapper = mock(ContestMapper.class);
        ContestKoreaPosterStorageService posterStorage = mock(ContestKoreaPosterStorageService.class);
        MediaImageService media = mock(MediaImageService.class);
        return new Fixture(mapper, posterStorage, media, new ContestKoreaUpsertService(mapper, posterStorage, media));
    }

    private Map<String, Object> existing(String representativeImagePath) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("contestId", 31L);
        row.put("sourceName", "CONTESTKOREA");
        row.put("sourceExternalId", "202606170004");
        row.put("representativeImagePath", representativeImagePath);
        return row;
    }

    private Map<String, Object> existingMatching(String representativeImagePath) {
        Map<String, Object> row = existing(representativeImagePath);
        row.put("contestType", "EXTERNAL");
        row.put("title", "2026 Slate 단편영화 공모전");
        row.put("summary", "단편영화와 영상 콘텐츠를 모집합니다.");
        row.put("theme", "사진•영상•영화제");
        row.put("prizeText", "총상금 1,000만원 / 대상 500만원");
        row.put("totalPrizeAmount", 10_000_000L);
        row.put("firstPrizeAmount", 5_000_000L);
        row.put("organizer", "한국영상협회");
        row.put("organizerType", "SCHOOL_ASSOCIATION");
        row.put("representativeImageUrl", null);
        row.put("posterSourceType", "CONTESTKOREA_ALLOWED");
        row.put("posterOriginalUrl", POSTER_URL);
        row.put("posterCollectedAt", LocalDateTime.of(2026, 6, 20, 10, 0));
        row.put("externalUrl", "https://apply.example.com/slate-film");
        row.put("targetText", "대학생 및 일반인");
        row.put("targetCodesJson", "[\"UNIVERSITY\",\"ADULT\"]");
        row.put("regionCodesJson", "[\"SEOUL\"]");
        row.put("requiredRolesText", "연출, 촬영");
        row.put("relatedGenresText", "영화");
        row.put("startAt", LocalDateTime.of(2026, 6, 10, 0, 0));
        row.put("deadlineAt", LocalDateTime.of(2026, 7, 8, 23, 59, 59));
        row.put("status", "OPEN");
        row.put("sourceUrl", "https://www.contestkorea.com/sub/view.php?str_no=202606170004");
        row.put("sourceCategoryCode", "031210001");
        row.put("sourcePermissionText", "콘테스트코리아 출처 표기 후 포스터 사용 가능");
        row.put("sourceAttribution", "출처: 콘테스트코리아");
        return row;
    }

    private ContestKoreaPosterStorageResult storedPoster(String representativeImagePath) {
        return new ContestKoreaPosterStorageResult(
                true,
                representativeImagePath,
                POSTER_URL,
                POSTER_COLLECTED_AT,
                "CONTESTKOREA_ALLOWED",
                "콘테스트코리아 출처 표기 후 포스터 사용 가능",
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaPosterStorageResult skippedAllowedPoster() {
        return new ContestKoreaPosterStorageResult(
                false,
                null,
                null,
                null,
                "CONTESTKOREA_ALLOWED",
                "콘테스트코리아 출처 표기 후 포스터 사용 가능",
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaPosterStorageResult permissionClearedPoster() {
        return new ContestKoreaPosterStorageResult(
                false,
                null,
                null,
                null,
                "NONE",
                null,
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaNormalizedContest normalized() {
        return normalized(builder -> { });
    }

    private ContestKoreaNormalizedContest normalized(java.util.function.Consumer<NormalizedBuilder> customizer) {
        NormalizedBuilder builder = new NormalizedBuilder();
        customizer.accept(builder);
        return builder.build();
    }

    private static final class NormalizedBuilder {
        String sourceName = "CONTESTKOREA";
        String sourceExternalId = "202606170004";

        ContestKoreaNormalizedContest build() {
            return new ContestKoreaNormalizedContest(
                    "EXTERNAL",
                    "2026 Slate 단편영화 공모전",
                    "단편영화와 영상 콘텐츠를 모집합니다.",
                    "사진•영상•영화제",
                    "총상금 1,000만원 / 대상 500만원",
                    10_000_000L,
                    5_000_000L,
                    "한국영상협회",
                    "SCHOOL_ASSOCIATION",
                    null,
                    "CONTESTKOREA_ALLOWED",
                    POSTER_URL,
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    "https://apply.example.com/slate-film",
                    "대학생 및 일반인",
                    List.of("UNIVERSITY", "ADULT"),
                    List.of("SEOUL"),
                    "연출, 촬영",
                    "영화",
                    LocalDateTime.of(2026, 6, 10, 0, 0),
                    LocalDateTime.of(2026, 7, 8, 23, 59, 59),
                    "OPEN",
                    sourceName,
                    sourceExternalId,
                    "https://www.contestkorea.com/sub/view.php?str_no=202606170004",
                    "031210001",
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    "콘테스트코리아 출처 표기 후 포스터 사용 가능",
                    "출처: 콘테스트코리아"
            );
        }
    }

    private record Fixture(
            ContestMapper mapper,
            ContestKoreaPosterStorageService posterStorage,
            MediaImageService media,
            ContestKoreaUpsertService service
    ) { }
}
