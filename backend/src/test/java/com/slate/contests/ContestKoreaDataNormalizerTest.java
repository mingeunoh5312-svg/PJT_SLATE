package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Consumer;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;

class ContestKoreaDataNormalizerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime FUTURE_DEADLINE = LocalDateTime.of(2026, 7, 8, 23, 59, 59);
    private static final LocalDateTime PAST_DEADLINE = LocalDateTime.of(2026, 6, 22, 23, 59, 59);

    private final ContestKoreaDataNormalizer normalizer = new ContestKoreaDataNormalizer(FIXED_CLOCK);

    @Test
    void normalizedContestRowContainsAllInsertContestKeys() {
        Map<String, Object> row = normalizer.normalize(parsed()).toRow();

        assertThat(row.keySet()).containsExactly(
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
                "posterCollectedAt",
                "submissionEmail",
                "externalUrl",
                "targetText",
                "targetCodesJson",
                "regionCodesJson",
                "requiredRolesText",
                "relatedGenresText",
                "startAt",
                "deadlineAt",
                "status",
                "createdBy",
                "requesterCompanyUserId",
                "sourceRequestId",
                "sourceName",
                "sourceExternalId",
                "sourceUrl",
                "sourceCategoryCode",
                "sourceCollectedAt",
                "sourceUpdatedAt",
                "sourcePermissionText",
                "sourceAttribution"
        );
    }

    @Test
    void targetTextMapsToTargetCodesJson() {
        Map<String, Object> row = row(builder -> builder.targetText = "누구나, 대학생, 일반인, 외국인");

        assertThat(row).containsEntry("targetCodesJson", "[\"ANYONE\",\"UNIVERSITY\",\"ADULT\",\"FOREIGNER\"]");
    }

    @Test
    void blankTargetTextUsesAnyoneDefault() {
        Map<String, Object> row = row(builder -> builder.targetText = " ");

        assertThat(row).containsEntry("targetCodesJson", "[\"ANYONE\"]");
    }

    @Test
    void onlineRegionMapsToOnlineCode() {
        Map<String, Object> row = row(builder -> builder.regionText = "온라인");

        assertThat(row).containsEntry("regionCodesJson", "[\"ONLINE\"]");
    }

    @Test
    void blankRegionTextUsesAllDefault() {
        Map<String, Object> row = row(builder -> builder.regionText = null);

        assertThat(row).containsEntry("regionCodesJson", "[\"ALL\"]");
    }

    @Test
    void companyOrganizerTypeIsInferredFromOrganizerName() {
        assertThat(normalizer.normalize(parsed(builder -> builder.organizer = "(주)대교")).organizerType())
                .isEqualTo("COMPANY");
        assertThat(normalizer.normalize(parsed(builder -> builder.organizer = "한솔그룹")).organizerType())
                .isEqualTo("COMPANY");
    }

    @Test
    void governmentOrganizerTypeIsInferredFromOrganizerName() {
        assertThat(normalizer.normalize(parsed(builder -> builder.organizer = "서울시")).organizerType())
                .isEqualTo("GOVERNMENT_PUBLIC");
        assertThat(normalizer.normalize(parsed(builder -> builder.organizer = "경기도교육청")).organizerType())
                .isEqualTo("GOVERNMENT_PUBLIC");
    }

    @Test
    void prizeTextExtractsTotalAndFirstPrizeAmounts() {
        ContestKoreaNormalizedContest contest = normalizer.normalize(parsed(builder -> {
            builder.prizeText = "총상금 800만원 / 1위 500만원";
            builder.totalPrizeAmount = null;
            builder.firstPrizeAmount = null;
        }));

        assertThat(contest.totalPrizeAmount()).isEqualTo(8_000_000L);
        assertThat(contest.firstPrizeAmount()).isEqualTo(5_000_000L);
    }

    @Test
    void invertedParsedPrizeAmountsAreNormalizedWithoutFailing() {
        ContestKoreaNormalizedContest contest = normalizer.normalize(parsed(builder -> {
            builder.prizeText = "1등 500만원 / 총상금 1,000만원";
            builder.totalPrizeAmount = 5_000_000L;
            builder.firstPrizeAmount = 10_000_000L;
        }));

        assertThat(contest.totalPrizeAmount()).isEqualTo(10_000_000L);
        assertThat(contest.firstPrizeAmount()).isEqualTo(5_000_000L);
    }

    @Test
    void pastDeadlineIsNormalizedToEnded() {
        Map<String, Object> row = row(builder -> builder.deadlineAt = PAST_DEADLINE);

        assertThat(row).containsEntry("status", "ENDED");
    }

    @Test
    void futureDeadlineIsNormalizedToOpen() {
        Map<String, Object> row = row(builder -> builder.deadlineAt = FUTURE_DEADLINE);

        assertThat(row).containsEntry("status", "OPEN");
    }

    @Test
    void missingSourceNameExternalIdOrUrlFails() {
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.sourceName = " ")))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.sourceExternalId = null)))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.sourceUrl = "file:///tmp/detail.html")))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void missingRequiredContestFieldsFail() {
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.title = " ")))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.summary = null)))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.organizer = " ")))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> normalizer.normalize(parsed(builder -> builder.deadlineAt = null)))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void posterNoneClearsPosterUrlAndCollectedAt() {
        Map<String, Object> row = row(builder -> builder.posterSourceType = "NONE");

        assertThat(row)
                .containsEntry("posterSourceType", "NONE")
                .containsEntry("posterOriginalUrl", null)
                .containsEntry("posterCollectedAt", null);
    }

    @Test
    void missingPermissionTextDisablesAllowedPosterType() {
        Map<String, Object> row = row(builder -> {
            builder.sourcePermissionText = null;
            builder.posterSourceType = "CONTESTKOREA_ALLOWED";
        });

        assertThat(row)
                .containsEntry("posterSourceType", "NONE")
                .containsEntry("posterOriginalUrl", null)
                .containsEntry("posterCollectedAt", null);
    }

    @Test
    void allowedPosterTypeIsNormalizedCaseInsensitively() {
        Map<String, Object> row = row(builder -> builder.posterSourceType = "contestkorea_allowed");

        assertThat(row)
                .containsEntry("posterSourceType", "CONTESTKOREA_ALLOWED")
                .containsEntry("posterOriginalUrl", "https://www.contestkorea.com/upload/contest/poster-2026.jpg");
    }

    @Test
    void unsafeExternalAndPosterUrlsAreSanitized() {
        Map<String, Object> row = row(builder -> {
            builder.externalUrl = "javascript:alert(1)";
            builder.posterOriginalUrl = "data:image/png;base64,AAAA";
        });

        assertThat(row)
                .containsEntry("externalUrl", null)
                .containsEntry("posterOriginalUrl", null)
                .containsEntry("posterCollectedAt", null);
    }

    @Test
    void officialExternalUrlIsPreservedAndSourceUrlStaysSeparate() {
        Map<String, Object> row = row(builder -> builder.externalUrl = "https://apply.example.com/slate-film");

        assertThat(row)
                .containsEntry("externalUrl", "https://apply.example.com/slate-film")
                .containsEntry("sourceUrl", "https://www.contestkorea.com/sub/view.php?str_no=202606170004");
    }

    @Test
    void missingExternalUrlDoesNotFallbackToSourceUrl() {
        Map<String, Object> row = row(builder -> builder.externalUrl = null);

        assertThat(row)
                .containsEntry("externalUrl", null)
                .containsEntry("sourceUrl", "https://www.contestkorea.com/sub/view.php?str_no=202606170004");
    }

    @Test
    void sourceUrlOrContestKoreaExternalUrlIsNotTreatedAsOfficialLink() {
        Map<String, Object> sameAsSource = row(builder -> builder.externalUrl = builder.sourceUrl);
        Map<String, Object> contestKoreaHost = row(builder -> builder.externalUrl = "https://contestkorea.com/sub/view.php?str_no=other");

        assertThat(sameAsSource).containsEntry("externalUrl", null);
        assertThat(contestKoreaHost).containsEntry("externalUrl", null);
    }

    @Test
    void textLengthLimitsAreApplied() {
        ContestKoreaNormalizedContest contest = normalizer.normalize(parsed(builder -> {
            builder.title = "가".repeat(250);
            builder.summary = "요".repeat(600);
            builder.theme = "분".repeat(180);
            builder.prizeText = "총상금 " + "상".repeat(180);
            builder.organizer = "주".repeat(150);
            builder.sourcePermissionText = "허".repeat(1_100);
        }));

        assertThat(contest.title()).hasSize(200);
        assertThat(contest.summary()).hasSize(500);
        assertThat(contest.theme()).hasSize(150);
        assertThat(contest.prizeText()).hasSize(150);
        assertThat(contest.organizer()).hasSize(120);
        assertThat(contest.sourcePermissionText()).hasSize(1_000);
    }

    private Map<String, Object> row(Consumer<ParsedBuilder> customizer) {
        return normalizer.normalize(parsed(customizer)).toRow();
    }

    private ContestKoreaParsedContest parsed() {
        return parsed(builder -> { });
    }

    private ContestKoreaParsedContest parsed(Consumer<ParsedBuilder> customizer) {
        ParsedBuilder builder = new ParsedBuilder();
        customizer.accept(builder);
        return builder.build();
    }

    private static final class ParsedBuilder {
        String contestType = "EXTERNAL";
        String status = "OPEN";
        String title = "2026 Slate 단편영화 공모전";
        String summary = "단편영화와 영상 콘텐츠를 모집합니다.";
        String theme = "사진•영상•영화제";
        String prizeText = "총상금 1,000만원 / 대상 500만원";
        Long totalPrizeAmount = 10_000_000L;
        Long firstPrizeAmount = 5_000_000L;
        String organizer = "한국영상협회";
        String organizerType;
        String representativeImageUrl;
        String targetText = "대학생 및 일반인";
        String regionText = "서울";
        String requiredRolesText = "연출, 촬영";
        String relatedGenresText = "영화";
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 10, 0, 0);
        LocalDateTime deadlineAt = FUTURE_DEADLINE;
        String externalUrl = "https://apply.example.com/slate-film";
        String sourceName = "CONTESTKOREA";
        String sourceExternalId = "202606170004";
        String sourceUrl = "https://www.contestkorea.com/sub/view.php?str_no=202606170004";
        String sourceCategoryCode = "031210001";
        LocalDateTime sourceCollectedAt = LocalDateTime.of(2026, 6, 23, 11, 0);
        LocalDateTime sourceUpdatedAt = sourceCollectedAt;
        String sourcePermissionText = "콘테스트코리아 출처 표기 후 포스터 사용 가능";
        String sourceAttribution = "출처: 콘테스트코리아";
        String posterSourceType = "CONTESTKOREA_ALLOWED";
        String posterOriginalUrl = "https://www.contestkorea.com/upload/contest/poster-2026.jpg";
        LocalDateTime posterCollectedAt = sourceCollectedAt;

        ContestKoreaParsedContest build() {
            return new ContestKoreaParsedContest(
                    contestType,
                    status,
                    title,
                    summary,
                    theme,
                    prizeText,
                    totalPrizeAmount,
                    firstPrizeAmount,
                    organizer,
                    organizerType,
                    representativeImageUrl,
                    targetText,
                    regionText,
                    requiredRolesText,
                    relatedGenresText,
                    startAt,
                    deadlineAt,
                    externalUrl,
                    sourceName,
                    sourceExternalId,
                    sourceUrl,
                    sourceCategoryCode,
                    sourceCollectedAt,
                    sourceUpdatedAt,
                    sourcePermissionText,
                    sourceAttribution,
                    posterSourceType,
                    posterOriginalUrl,
                    posterCollectedAt
            );
        }
    }
}
