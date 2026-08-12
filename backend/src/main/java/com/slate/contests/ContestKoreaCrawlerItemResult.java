package com.slate.contests;

public record ContestKoreaCrawlerItemResult(
        String sourceExternalId,
        String detailUrl,
        String status,
        boolean inserted,
        boolean updated,
        boolean posterStored,
        Long contestId,
        String representativeImagePath,
        String message,
        String stage
) {
    public static final String STATUS_INSERTED = "INSERTED";
    public static final String STATUS_UPDATED = "UPDATED";
    public static final String STATUS_DRY_RUN = "DRY_RUN";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    static ContestKoreaCrawlerItemResult saved(ContestKoreaListItem item, ContestKoreaUpsertResult result) {
        String status = result.inserted() ? STATUS_INSERTED : result.updated() ? STATUS_UPDATED : STATUS_SKIPPED;
        return new ContestKoreaCrawlerItemResult(
                result.sourceExternalId(),
                item.detailUrl(),
                status,
                result.inserted(),
                result.updated(),
                result.posterStored(),
                result.contestId(),
                result.representativeImagePath(),
                STATUS_SKIPPED.equals(status) ? "기존 공모전과 변경 사항이 없어 갱신을 생략했습니다." : null,
                STATUS_SKIPPED.equals(status) ? "UPSERT_SKIP" : null
        );
    }

    static ContestKoreaCrawlerItemResult dryRun(ContestKoreaListItem item, ContestKoreaNormalizedContest normalized) {
        return new ContestKoreaCrawlerItemResult(
                normalized.sourceExternalId(),
                item.detailUrl(),
                STATUS_DRY_RUN,
                false,
                false,
                false,
                null,
                null,
                "dryRun",
                null
        );
    }

    static ContestKoreaCrawlerItemResult skipped(ContestKoreaListItem item, String stage, String message) {
        return new ContestKoreaCrawlerItemResult(
                item.sourceExternalId(),
                item.detailUrl(),
                STATUS_SKIPPED,
                false,
                false,
                false,
                null,
                null,
                message,
                stage
        );
    }

    static ContestKoreaCrawlerItemResult failed(ContestKoreaListItem item, String stage, String message) {
        return new ContestKoreaCrawlerItemResult(
                item.sourceExternalId(),
                item.detailUrl(),
                STATUS_FAILED,
                false,
                false,
                false,
                null,
                null,
                message,
                stage
        );
    }
}
