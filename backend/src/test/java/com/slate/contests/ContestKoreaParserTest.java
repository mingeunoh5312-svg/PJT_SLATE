package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;

class ContestKoreaParserTest {

    private static final String DETAIL_URL = "https://www.contestkorea.com/sub/view.php?Txt_bcode=031210001&int_gbn=1&str_no=202606170004";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void listParserExtractsDetailUrlSourceExternalIdAndRemovesDuplicates() throws Exception {
        List<ContestKoreaListItem> items = parser(true).parseList(resource("contestkorea/list_sample.html"));

        assertThat(items).hasSize(1);
        ContestKoreaListItem item = items.get(0);
        assertThat(item.detailUrl())
                .isEqualTo("https://www.contestkorea.com/sub/view.php?Txt_bcode=031210001&int_gbn=1&str_no=202606170004");
        assertThat(item.sourceExternalId()).isEqualTo("202606170004");
        assertThat(item.sourceCategoryCode()).isEqualTo("031210001");
        assertThat(item.title()).isEqualTo("2026 Slate 단편영화 공모전");
        assertThat(item.organizer()).isEqualTo("한국영상협회");
        assertThat(item.targetText()).isEqualTo("대학생");
    }

    @Test
    void listParserKeepsOnlyPhotoVideoFilmCategory() throws Exception {
        List<ContestKoreaListItem> items = parser(true).parseList(resource("contestkorea/list_sample.html"));

        assertThat(items)
                .extracting(ContestKoreaListItem::title)
                .containsExactly("2026 Slate 단편영화 공모전")
                .doesNotContain("문학 공모전");
    }

    @Test
    void listParserSkipsDetailLinksWithoutConfirmedPhotoVideoFilmCategory() {
        String html = """
                <html><body>
                  <a href="/sub/view.php?Txt_bcode=031210001&amp;int_gbn=1&amp;str_no=202606170004">카테고리 없는 추천 공모전</a>
                  <li>
                    <span class="category">사진•영상•영화제</span>
                    <a href="/sub/view.php?Txt_bcode=031210001&amp;int_gbn=1&amp;str_no=202606170005">영상 공모전</a>
                  </li>
                </body></html>
                """;

        assertThat(parser(true).parseList(html))
                .extracting(ContestKoreaListItem::sourceExternalId)
                .containsExactly("202606170005");
    }

    @Test
    void listParserReturnsEmptyListWhenHtmlHasNoDetailLinks() {
        assertThat(parser(true).parseList("<html><body><a href=\"/sub/list.php\">목록</a></body></html>")).isEmpty();
        assertThat(parser(true).parseList(" ")).isEmpty();
    }

    @Test
    void detailParserExtractsRequiredContestFields() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(resource("contestkorea/detail_allowed_sample.html"), DETAIL_URL);

        assertThat(contest.title()).isEqualTo("2026 Slate 단편영화 공모전");
        assertThat(contest.organizer()).isEqualTo("한국영상협회");
        assertThat(contest.targetText()).isEqualTo("대학생 및 청년 창작자");
        assertThat(contest.theme()).isEqualTo("사진•영상•영화제");
        assertThat(contest.prizeText()).isEqualTo("총상금 1,000만원 / 대상 500만원");
        assertThat(contest.totalPrizeAmount()).isEqualTo(10_000_000L);
        assertThat(contest.firstPrizeAmount()).isEqualTo(5_000_000L);
        assertThat(contest.startAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 0, 0));
        assertThat(contest.deadlineAt()).isEqualTo(LocalDateTime.of(2026, 7, 8, 23, 59, 59));
        assertThat(contest.summary()).contains("단편영화와 영상 콘텐츠");
    }

    @Test
    void detailParserFillsSourceMetadata() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(resource("contestkorea/detail_allowed_sample.html"), DETAIL_URL);

        assertThat(contest.contestType()).isEqualTo("EXTERNAL");
        assertThat(contest.status()).isEqualTo("OPEN");
        assertThat(contest.sourceName()).isEqualTo("CONTESTKOREA");
        assertThat(contest.sourceExternalId()).isEqualTo("202606170004");
        assertThat(contest.sourceUrl()).isEqualTo(DETAIL_URL);
        assertThat(contest.sourceCategoryCode()).isEqualTo("031210001");
        assertThat(contest.sourceAttribution()).isEqualTo("출처: 콘테스트코리아");
        assertThat(contest.sourceCollectedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 11, 0));
        assertThat(contest.sourceUpdatedAt()).isEqualTo(contest.sourceCollectedAt());
    }

    @Test
    void permissionTextEnablesPosterMetadata() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(resource("contestkorea/detail_allowed_sample.html"), DETAIL_URL);

        assertThat(contest.sourcePermissionText()).contains("콘테스트코리아 출처 표기");
        assertThat(contest.posterSourceType()).isEqualTo("CONTESTKOREA_ALLOWED");
        assertThat(contest.posterOriginalUrl()).isEqualTo("https://www.contestkorea.com/upload/contest/poster-2026.jpg");
        assertThat(contest.posterCollectedAt()).isEqualTo(contest.sourceCollectedAt());
        assertThat(contest.representativeImageUrl()).isNull();
    }

    @Test
    void missingPermissionTextDisablesPosterMetadata() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(
                resource("contestkorea/detail_no_permission_sample.html"),
                "https://www.contestkorea.com/sub/view.php?str_no=202606170006"
        );

        assertThat(contest.posterSourceType()).isEqualTo("NONE");
        assertThat(contest.posterOriginalUrl()).isNull();
        assertThat(contest.posterCollectedAt()).isNull();
        assertThat(contest.sourcePermissionText()).isNull();
    }

    @Test
    void posterDownloadDisabledKeepsPermissionButClearsPosterUrl() throws Exception {
        ContestKoreaParsedContest contest = parser(false).parseDetail(resource("contestkorea/detail_allowed_sample.html"), DETAIL_URL);

        assertThat(contest.sourcePermissionText()).contains("콘테스트코리아 출처 표기");
        assertThat(contest.posterSourceType()).isEqualTo("CONTESTKOREA_ALLOWED");
        assertThat(contest.posterOriginalUrl()).isNull();
        assertThat(contest.posterCollectedAt()).isNull();
    }

    @Test
    void detailParserScopesLivePageChromeAwayFromTitleAndPoster() {
        String html = """
                <!doctype html>
                <html>
                <head>
                  <meta property="og:title" content="OG fallback title">
                  <meta property="og:image" content="http://www.contestkorea.com/admincenter/files/meet/fallback.jpg">
                </head>
                <body>
                  <div class="header">
                    <h1>현재 진행 수</h1>
                    <img src="/img/common/home_BI_B.gif" alt="logo">
                    <img src="/admincenter/files/com/insta_contestkorea.png" alt="sns">
                  </div>
                  <div class="view_cont_area">
                    <div class="view_top_area">
                      <h1>2026 한국마사회 AI 영상 공모전</h1>
                      <div class="img_area"><img src="/admincenter/files/meet/202606171351502641986.jpg" alt="poster"></div>
                    </div>
                    <table>
                      <tr><th>주최</th><td>한국마사회</td></tr>
                      <tr><th>대표분야</th><td>사진•영상•영화제</td></tr>
                      <tr><th>참가대상</th><td>누구나</td></tr>
                      <tr><th>접수기간</th><td>2026. 6. 15 ~ 2026. 8. 15</td></tr>
                      <tr><th>시상내역</th><td>대상 500만원</td></tr>
                    </table>
                    <div class="view_detail_area">
                      <p>%s</p>
                      <p>본 게시물은 [콘테스트코리아 출처 표기] 시 자유롭게 복사 및 배포하실 수 있습니다.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted("상세 안내 ".repeat(180));

        ContestKoreaParsedContest contest = parser(true).parseDetail(html, DETAIL_URL);

        assertThat(contest.title()).isEqualTo("2026 한국마사회 AI 영상 공모전");
        assertThat(contest.posterSourceType()).isEqualTo("CONTESTKOREA_ALLOWED");
        assertThat(contest.posterOriginalUrl()).isEqualTo("https://www.contestkorea.com/admincenter/files/meet/202606171351502641986.jpg");
        assertThat(contest.sourcePermissionText()).contains("콘테스트코리아 출처 표기");
    }

    @Test
    void detailParserExtractsExternalApplyUrlAndIgnoresUnsafeUrls() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(resource("contestkorea/detail_allowed_sample.html"), DETAIL_URL);

        assertThat(contest.externalUrl()).isEqualTo("https://apply.example.com/slate-film");
        assertThat(contest.posterOriginalUrl()).doesNotContain("javascript", "data:", "file:");
    }

    @Test
    void detailParserIgnoresContestKoreaInternalLinksAsExternalApplyUrl() {
        String html = detailHtml("""
                <a href="https://www.contestkorea.com/sub/view.php?str_no=internal">공식 공고 보기</a>
                <a href="https://contestkorea.com/sub/list.php">접수 바로가기</a>
                """);

        ContestKoreaParsedContest contest = parser(true).parseDetail(html, DETAIL_URL);

        assertThat(contest.externalUrl()).isNull();
    }

    @Test
    void detailParserIgnoresSnsShareAdsAndDownloadLinksAsExternalApplyUrl() {
        String html = detailHtml("""
                <a href="https://facebook.com/share/slate">공유하기</a>
                <a href="https://instagram.com/slate">SNS</a>
                <a href="https://ads.example.com/banner">광고 바로가기</a>
                <a href="https://files.example.com/slate-guide.pdf">공고 다운로드</a>
                """);

        ContestKoreaParsedContest contest = parser(true).parseDetail(html, DETAIL_URL);

        assertThat(contest.externalUrl()).isNull();
    }

    @Test
    void detailParserUsesNearbyOfficialKeywordForExternalApplyUrl() {
        String html = detailHtml("""
                <p>주최사 공식 홈페이지 <a href="https://official.example.org/contest/slate">바로가기</a></p>
                """);

        ContestKoreaParsedContest contest = parser(true).parseDetail(html, DETAIL_URL);

        assertThat(contest.externalUrl()).isEqualTo("https://official.example.org/contest/slate");
    }

    @Test
    void koreanDateFormatParsesToLocalDateTime() throws Exception {
        ContestKoreaParsedContest contest = parser(true).parseDetail(
                resource("contestkorea/detail_no_permission_sample.html"),
                "https://www.contestkorea.com/sub/view.php?str_no=202606170006"
        );

        assertThat(contest.startAt()).isEqualTo(LocalDateTime.of(2026, 6, 17, 0, 0));
        assertThat(contest.deadlineAt()).isEqualTo(LocalDateTime.of(2026, 7, 8, 23, 59));
    }

    @Test
    void unparseableDeadlineFailsDetailParsing() {
        String html = """
                <html><body>
                  <h1>잘못된 공모전</h1>
                  <dl>
                    <dt>주최</dt><dd>테스트</dd>
                    <dt>접수기간</dt><dd>언젠가 마감</dd>
                  </dl>
                </body></html>
                """;

        assertThatThrownBy(() -> parser(true).parseDetail(html, "https://www.contestkorea.com/sub/view.php?str_no=bad-date"))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("접수기간");
    }

    private ContestKoreaParser parser(boolean posterDownloadEnabled) {
        return new ContestKoreaParser(properties(posterDownloadEnabled), FIXED_CLOCK);
    }

    private ContestKoreaProperties properties(boolean posterDownloadEnabled) {
        return new ContestKoreaProperties(
                true,
                "https://www.contestkorea.com",
                "/sub/list.php",
                "031210001",
                1,
                "SlateBot/1.0",
                1500L,
                5000L,
                10000L,
                1,
                30,
                posterDownloadEnabled,
                "콘테스트코리아 출처 표기",
                "CONTESTKOREA",
                "출처: 콘테스트코리아"
        );
    }

    private String detailHtml(String links) {
        return """
                <!doctype html>
                <html>
                <body>
                  <article class="contest-detail">
                    <h1>2026 Slate 공식 링크 테스트 공모전</h1>
                    <dl class="info">
                      <dt>주최</dt><dd>한국영상협회</dd>
                      <dt>대표분야</dt><dd>사진•영상•영화제</dd>
                      <dt>참가대상</dt><dd>대학생</dd>
                      <dt>접수기간</dt><dd>2026.06.10 ~ 2026.07.08</dd>
                      <dt>시상내역</dt><dd>대상 500만원</dd>
                    </dl>
                    <p>단편영화와 영상 콘텐츠를 모집합니다.</p>
                    %s
                  </article>
                </body>
                </html>
                """.formatted(links);
    }

    private String resource(String path) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
