package com.slate.contests;

import java.time.LocalDateTime;

public record ContestKoreaParsedContest(
        String contestType,
        String status,
        String title,
        String summary,
        String theme,
        String prizeText,
        Long totalPrizeAmount,
        Long firstPrizeAmount,
        String organizer,
        String organizerType,
        String representativeImageUrl,
        String targetText,
        String regionText,
        String requiredRolesText,
        String relatedGenresText,
        LocalDateTime startAt,
        LocalDateTime deadlineAt,
        String externalUrl,
        String sourceName,
        String sourceExternalId,
        String sourceUrl,
        String sourceCategoryCode,
        LocalDateTime sourceCollectedAt,
        LocalDateTime sourceUpdatedAt,
        String sourcePermissionText,
        String sourceAttribution,
        String posterSourceType,
        String posterOriginalUrl,
        LocalDateTime posterCollectedAt
) {
}
