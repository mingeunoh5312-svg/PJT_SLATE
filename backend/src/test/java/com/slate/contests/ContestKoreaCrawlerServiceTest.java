package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ContestKoreaCrawlerServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void disabledCrawlerFailsBeforeClientCall() {
        Fixture fixture = fixture(false, 1, 30);

        assertThatThrownBy(() -> fixture.service.run())
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("비활성화");

        verifyNoInteractions(fixture.client);
    }

    @Test
    void nullRequestUsesConfiguredMaxPagesAndItems() {
        Fixture fixture = fixture(true, 2, 2);
        ContestKoreaListItem first = item("202606170001");
        ContestKoreaListItem second = item("202606170002");
        stubList(fixture, 1, List.of(first));
        stubList(fixture, 2, List.of(second));
        stubSuccess(fixture, first, inserted("202606170001", 41L, false));
        stubSuccess(fixture, second, updated("202606170002", 42L, true));

        ContestKoreaCrawlerRunResult result = fixture.service.run(null);

        verify(fixture.client).fetchListPage(1);
        verify(fixture.client).fetchListPage(2);
        assertThat(result.requestedMaxPages()).isEqualTo(2);
        assertThat(result.requestedMaxItems()).isEqualTo(2);
        assertThat(result.fetchedPages()).isEqualTo(2);
        assertThat(result.processedItems()).isEqualTo(2);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.posterStoredCount()).isEqualTo(1);
        assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 11, 0));
        assertThat(result.finishedAt()).isEqualTo(result.startedAt());
    }

    @Test
    void requestLimitsAreClampedAndDeduplicatedBeforeDetailProcessing() {
        Fixture fixture = fixture(true, 2, 1);
        ContestKoreaListItem first = item("202606170001");
        ContestKoreaListItem second = item("202606170002");
        ContestKoreaListItem duplicateFirst = item("202606170001");
        ContestKoreaListItem third = item("202606170003");
        stubList(fixture, 1, List.of(first, second));
        stubList(fixture, 2, List.of(duplicateFirst, third));
        stubSuccess(fixture, first, inserted("202606170001", 41L, false));

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(99, 99, false));

        InOrder listOrder = inOrder(fixture.client);
        listOrder.verify(fixture.client).fetchListPage(1);
        listOrder.verify(fixture.client).fetchListPage(2);
        verify(fixture.client).fetchDetailPage(first.detailUrl());
        verify(fixture.client, never()).fetchDetailPage(second.detailUrl());
        verify(fixture.client, never()).fetchDetailPage(third.detailUrl());
        assertThat(result.requestedMaxPages()).isEqualTo(2);
        assertThat(result.requestedMaxItems()).isEqualTo(1);
        assertThat(result.discoveredItems()).isEqualTo(4);
        assertThat(result.deduplicatedItems()).isEqualTo(3);
        assertThat(result.processedItems()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(2);
    }

    @Test
    void successfulItemRunsDetailFetchParseNormalizeAndUpsertInOrder() {
        Fixture fixture = fixture(true, 1, 1);
        ContestKoreaListItem item = item("202606170001");
        ContestKoreaParsedContest parsed = parsed("202606170001");
        ContestKoreaNormalizedContest normalized = normalized("202606170001");
        when(fixture.client.fetchListPage(1)).thenReturn("list-1");
        when(fixture.parser.parseList("list-1")).thenReturn(List.of(item));
        when(fixture.client.fetchDetailPage(item.detailUrl())).thenReturn("detail-1");
        when(fixture.parser.parseDetail("detail-1", item.detailUrl())).thenReturn(parsed);
        when(fixture.normalizer.normalize(parsed)).thenReturn(normalized);
        when(fixture.upsertService.upsert(normalized)).thenReturn(inserted("202606170001", 41L, true));

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(1, 1, false));

        InOrder order = inOrder(fixture.client, fixture.parser, fixture.normalizer, fixture.upsertService);
        order.verify(fixture.client).fetchListPage(1);
        order.verify(fixture.parser).parseList("list-1");
        order.verify(fixture.client).fetchDetailPage(item.detailUrl());
        order.verify(fixture.parser).parseDetail("detail-1", item.detailUrl());
        order.verify(fixture.normalizer).normalize(parsed);
        order.verify(fixture.upsertService).upsert(normalized);
        assertThat(result.itemResults()).singleElement()
                .satisfies(itemResult -> {
                    assertThat(itemResult.status()).isEqualTo(ContestKoreaCrawlerItemResult.STATUS_INSERTED);
                    assertThat(itemResult.contestId()).isEqualTo(41L);
                    assertThat(itemResult.representativeImagePath()).isEqualTo("images/contest/2026/06/202606170001.jpg");
                });
    }

    @Test
    void dryRunSkipsUpsertAndRecordsDryRunStatus() {
        Fixture fixture = fixture(true, 1, 1);
        ContestKoreaListItem item = item("202606170001");
        stubList(fixture, 1, List.of(item));
        stubParsedAndNormalized(fixture, item);

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(1, 1, true));

        verify(fixture.upsertService, never()).upsert(normalized("202606170001"));
        assertThat(result.dryRun()).isTrue();
        assertThat(result.insertedCount()).isZero();
        assertThat(result.itemResults()).singleElement()
                .extracting(ContestKoreaCrawlerItemResult::status)
                .isEqualTo(ContestKoreaCrawlerItemResult.STATUS_DRY_RUN);
    }

    @Test
    void photoOnlyContestIsSkippedBeforeNormalize() {
        Fixture fixture = fixture(true, 1, 1);
        ContestKoreaListItem item = item("202606170001");
        ContestKoreaParsedContest parsed = parsed(
                "202606170001",
                "가족 사진 공모전",
                "일상 사진 작품을 모집합니다. 필름 사진과 스마트폰 사진 모두 접수 가능합니다."
        );
        stubList(fixture, 1, List.of(item));
        when(fixture.client.fetchDetailPage(item.detailUrl())).thenReturn("photo-detail");
        when(fixture.parser.parseDetail("photo-detail", item.detailUrl())).thenReturn(parsed);

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(1, 1, false));

        verify(fixture.normalizer, never()).normalize(parsed);
        verifyNoInteractions(fixture.upsertService);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.itemResults()).singleElement()
                .satisfies(itemResult -> {
                    assertThat(itemResult.status()).isEqualTo(ContestKoreaCrawlerItemResult.STATUS_SKIPPED);
                    assertThat(itemResult.stage()).isEqualTo("CONTENT_FILTER");
                    assertThat(itemResult.message()).contains("사진 단일 주제");
                });
    }

    @Test
    void photoAndVideoContestIsNotSkipped() {
        Fixture fixture = fixture(true, 1, 1);
        ContestKoreaListItem item = item("202606170001");
        ContestKoreaParsedContest parsed = parsed(
                "202606170001",
                "사진 영상 공모전",
                "사진과 영상 콘텐츠를 함께 모집합니다."
        );
        ContestKoreaNormalizedContest normalized = normalized("202606170001");
        stubList(fixture, 1, List.of(item));
        when(fixture.client.fetchDetailPage(item.detailUrl())).thenReturn("photo-video-detail");
        when(fixture.parser.parseDetail("photo-video-detail", item.detailUrl())).thenReturn(parsed);
        when(fixture.normalizer.normalize(parsed)).thenReturn(normalized);
        when(fixture.upsertService.upsert(normalized)).thenReturn(inserted("202606170001", 41L, false));

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(1, 1, false));

        verify(fixture.normalizer).normalize(parsed);
        verify(fixture.upsertService).upsert(normalized);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
    }

    @Test
    void itemFailuresRecordStageAndContinueNextItems() {
        Fixture fixture = fixture(true, 1, 5);
        ContestKoreaListItem detailFail = item("202606170001");
        ContestKoreaListItem parseFail = item("202606170002");
        ContestKoreaListItem normalizeFail = item("202606170003");
        ContestKoreaListItem upsertFail = item("202606170004");
        ContestKoreaListItem success = item("202606170005");
        stubList(fixture, 1, List.of(detailFail, parseFail, normalizeFail, upsertFail, success));

        when(fixture.client.fetchDetailPage(detailFail.detailUrl())).thenThrow(new SlateException("detail down"));

        when(fixture.client.fetchDetailPage(parseFail.detailUrl())).thenReturn("parse-detail");
        when(fixture.parser.parseDetail("parse-detail", parseFail.detailUrl())).thenThrow(new SlateException("parse down"));

        ContestKoreaParsedContest normalizeParsed = parsed("202606170003");
        when(fixture.client.fetchDetailPage(normalizeFail.detailUrl())).thenReturn("normalize-detail");
        when(fixture.parser.parseDetail("normalize-detail", normalizeFail.detailUrl())).thenReturn(normalizeParsed);
        when(fixture.normalizer.normalize(normalizeParsed)).thenThrow(new SlateException("normalize down"));

        ContestKoreaParsedContest upsertParsed = parsed("202606170004");
        ContestKoreaNormalizedContest upsertNormalized = normalized("202606170004");
        when(fixture.client.fetchDetailPage(upsertFail.detailUrl())).thenReturn("upsert-detail");
        when(fixture.parser.parseDetail("upsert-detail", upsertFail.detailUrl())).thenReturn(upsertParsed);
        when(fixture.normalizer.normalize(upsertParsed)).thenReturn(upsertNormalized);
        when(fixture.upsertService.upsert(upsertNormalized)).thenThrow(new SlateException("upsert down"));

        stubSuccess(fixture, success, inserted("202606170005", 45L, false));

        ContestKoreaCrawlerRunResult result = fixture.service.run(new ContestKoreaCrawlerRunRequest(1, 5, false));

        assertThat(result.processedItems()).isEqualTo(5);
        assertThat(result.failedCount()).isEqualTo(4);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.itemResults())
                .extracting(ContestKoreaCrawlerItemResult::stage)
                .containsExactly("DETAIL_FETCH", "DETAIL_PARSE", "NORMALIZE", "UPSERT", null);
    }

    @Test
    void listFetchFailureFailsWholeRun() {
        Fixture fixture = fixture(true, 2, 10);
        when(fixture.client.fetchListPage(1)).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> fixture.service.run(new ContestKoreaCrawlerRunRequest(2, 10, false)))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("목록 페이지 조회");

        verify(fixture.client).fetchListPage(1);
        verify(fixture.client, never()).fetchListPage(2);
        verifyNoInteractions(fixture.parser);
    }

    private void stubList(Fixture fixture, int page, List<ContestKoreaListItem> items) {
        String html = "list-" + page;
        when(fixture.client.fetchListPage(page)).thenReturn(html);
        when(fixture.parser.parseList(html)).thenReturn(items);
    }

    private void stubSuccess(Fixture fixture, ContestKoreaListItem item, ContestKoreaUpsertResult result) {
        ContestKoreaParsedContest parsed = parsed(item.sourceExternalId());
        ContestKoreaNormalizedContest normalized = normalized(item.sourceExternalId());
        when(fixture.client.fetchDetailPage(item.detailUrl())).thenReturn("detail-" + item.sourceExternalId());
        when(fixture.parser.parseDetail("detail-" + item.sourceExternalId(), item.detailUrl())).thenReturn(parsed);
        when(fixture.normalizer.normalize(parsed)).thenReturn(normalized);
        when(fixture.upsertService.upsert(normalized)).thenReturn(result);
    }

    private void stubParsedAndNormalized(Fixture fixture, ContestKoreaListItem item) {
        ContestKoreaParsedContest parsed = parsed(item.sourceExternalId());
        ContestKoreaNormalizedContest normalized = normalized(item.sourceExternalId());
        when(fixture.client.fetchDetailPage(item.detailUrl())).thenReturn("detail-" + item.sourceExternalId());
        when(fixture.parser.parseDetail("detail-" + item.sourceExternalId(), item.detailUrl())).thenReturn(parsed);
        when(fixture.normalizer.normalize(parsed)).thenReturn(normalized);
    }

    private Fixture fixture(boolean enabled, int maxPages, int maxItems) {
        ContestKoreaClient client = mock(ContestKoreaClient.class);
        ContestKoreaParser parser = mock(ContestKoreaParser.class);
        ContestKoreaDataNormalizer normalizer = mock(ContestKoreaDataNormalizer.class);
        ContestKoreaUpsertService upsertService = mock(ContestKoreaUpsertService.class);
        ContestKoreaCrawlerService service = new ContestKoreaCrawlerService(
                properties(enabled, maxPages, maxItems),
                client,
                parser,
                normalizer,
                upsertService,
                FIXED_CLOCK
        );
        return new Fixture(service, client, parser, normalizer, upsertService);
    }

    private ContestKoreaProperties properties(boolean enabled, int maxPages, int maxItems) {
        return new ContestKoreaProperties(
                enabled,
                "https://www.contestkorea.com",
                "/sub/list.php",
                "031210001",
                1,
                "SlateBot/1.0",
                1500L,
                5000L,
                10000L,
                maxPages,
                maxItems,
                true,
                "콘테스트코리아 출처 표기",
                "CONTESTKOREA",
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaListItem item(String sourceExternalId) {
        return new ContestKoreaListItem(
                "공모전 " + sourceExternalId,
                "한국영상협회",
                "대학생",
                "접수중",
                "2026.07.08",
                "https://www.contestkorea.com/sub/view.php?str_no=" + sourceExternalId,
                sourceExternalId,
                "031210001"
        );
    }

    private ContestKoreaParsedContest parsed(String sourceExternalId) {
        return parsed(sourceExternalId, "공모전 " + sourceExternalId, "영상 콘텐츠 공모전");
    }

    private ContestKoreaParsedContest parsed(String sourceExternalId, String title, String summary) {
        return new ContestKoreaParsedContest(
                "EXTERNAL",
                "OPEN",
                title,
                summary,
                "사진영상영화제",
                "총상금 100만원",
                1_000_000L,
                500_000L,
                "한국영상협회",
                null,
                null,
                "대학생",
                "서울",
                null,
                null,
                LocalDateTime.of(2026, 6, 10, 0, 0),
                LocalDateTime.of(2026, 7, 8, 23, 59, 59),
                "https://apply.example.com/" + sourceExternalId,
                "CONTESTKOREA",
                sourceExternalId,
                "https://www.contestkorea.com/sub/view.php?str_no=" + sourceExternalId,
                "031210001",
                LocalDateTime.of(2026, 6, 23, 11, 0),
                LocalDateTime.of(2026, 6, 23, 11, 0),
                "콘테스트코리아 출처 표기",
                "출처: 콘테스트코리아",
                "CONTESTKOREA_ALLOWED",
                "https://www.contestkorea.com/upload/contest/" + sourceExternalId + ".jpg",
                LocalDateTime.of(2026, 6, 23, 11, 0)
        );
    }

    private ContestKoreaNormalizedContest normalized(String sourceExternalId) {
        return new ContestKoreaNormalizedContest(
                "EXTERNAL",
                "공모전 " + sourceExternalId,
                "영상 콘텐츠 공모전",
                "사진영상영화제",
                "총상금 100만원",
                1_000_000L,
                500_000L,
                "한국영상협회",
                "SCHOOL_ASSOCIATION",
                null,
                "CONTESTKOREA_ALLOWED",
                "https://www.contestkorea.com/upload/contest/" + sourceExternalId + ".jpg",
                LocalDateTime.of(2026, 6, 23, 11, 0),
                "https://apply.example.com/" + sourceExternalId,
                "대학생",
                List.of("UNIVERSITY"),
                List.of("SEOUL"),
                null,
                null,
                LocalDateTime.of(2026, 6, 10, 0, 0),
                LocalDateTime.of(2026, 7, 8, 23, 59, 59),
                "OPEN",
                "CONTESTKOREA",
                sourceExternalId,
                "https://www.contestkorea.com/sub/view.php?str_no=" + sourceExternalId,
                "031210001",
                LocalDateTime.of(2026, 6, 23, 11, 0),
                LocalDateTime.of(2026, 6, 23, 11, 0),
                "콘테스트코리아 출처 표기",
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaUpsertResult inserted(String sourceExternalId, Long contestId, boolean posterStored) {
        return new ContestKoreaUpsertResult(
                contestId,
                "CONTESTKOREA",
                sourceExternalId,
                true,
                false,
                posterStored,
                "images/contest/2026/06/" + sourceExternalId + ".jpg"
        );
    }

    private ContestKoreaUpsertResult updated(String sourceExternalId, Long contestId, boolean posterStored) {
        return new ContestKoreaUpsertResult(
                contestId,
                "CONTESTKOREA",
                sourceExternalId,
                false,
                true,
                posterStored,
                "images/contest/2026/06/" + sourceExternalId + ".jpg"
        );
    }

    private record Fixture(
            ContestKoreaCrawlerService service,
            ContestKoreaClient client,
            ContestKoreaParser parser,
            ContestKoreaDataNormalizer normalizer,
            ContestKoreaUpsertService upsertService
    ) {
    }
}
