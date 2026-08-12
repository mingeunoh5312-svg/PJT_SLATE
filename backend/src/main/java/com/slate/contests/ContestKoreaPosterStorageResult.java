package com.slate.contests;

import java.time.LocalDateTime;

public record ContestKoreaPosterStorageResult(
        boolean stored,
        String representativeImagePath,
        String posterOriginalUrl,
        LocalDateTime posterCollectedAt,
        String posterSourceType,
        String sourcePermissionText,
        String sourceAttribution
) {
    private static final String POSTER_ALLOWED = "CONTESTKOREA_ALLOWED";

    public static ContestKoreaPosterStorageResult skipped(ContestKoreaNormalizedContest contest) {
        return new ContestKoreaPosterStorageResult(
                false,
                null,
                contest == null ? null : contest.posterOriginalUrl(),
                null,
                contest == null ? null : contest.posterSourceType(),
                contest == null ? null : contest.sourcePermissionText(),
                contest == null ? null : contest.sourceAttribution()
        );
    }

    public static ContestKoreaPosterStorageResult stored(
            String representativeImagePath,
            String posterOriginalUrl,
            LocalDateTime posterCollectedAt,
            ContestKoreaNormalizedContest contest
    ) {
        return new ContestKoreaPosterStorageResult(
                true,
                representativeImagePath,
                posterOriginalUrl,
                posterCollectedAt,
                POSTER_ALLOWED,
                contest.sourcePermissionText(),
                contest.sourceAttribution()
        );
    }
}
