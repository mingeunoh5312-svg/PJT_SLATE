package com.slate.contests;

import java.util.List;

public record ContestSearchCriteria(
        String status,
        String sort,
        String contestType,
        String keyword,
        Integer deadlineWithinDays,
        String regionMode,
        List<String> targetCodes,
        List<String> regionCodes,
        List<String> organizerTypes,
        Long totalPrizeMin,
        Long totalPrizeMax,
        Long firstPrizeMin,
        Long firstPrizeMax,
        int limit
) {
    public ContestSearchCriteria(
            String status,
            String sort,
            String contestType,
            String keyword,
            List<String> targetCodes,
            List<String> regionCodes,
            List<String> organizerTypes,
            Long totalPrizeMin,
            Long totalPrizeMax,
            Long firstPrizeMin,
            Long firstPrizeMax,
            int limit
    ) {
        this(
                status,
                sort,
                contestType,
                keyword,
                null,
                null,
                targetCodes,
                regionCodes,
                organizerTypes,
                totalPrizeMin,
                totalPrizeMax,
                firstPrizeMin,
                firstPrizeMax,
                limit
        );
    }
}
