package com.slate.contests;

public record ContestKoreaListItem(
        String title,
        String organizer,
        String targetText,
        String listStatusText,
        String listDeadlineHint,
        String detailUrl,
        String sourceExternalId,
        String sourceCategoryCode
) {
}
