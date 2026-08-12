package com.slate.contests;

import java.time.LocalDateTime;
import java.util.List;

public record ContestKoreaCrawlerRunResult(
        boolean enabled,
        boolean dryRun,
        int requestedMaxPages,
        int requestedMaxItems,
        int fetchedPages,
        int discoveredItems,
        int deduplicatedItems,
        int processedItems,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        int posterStoredCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<ContestKoreaCrawlerItemResult> itemResults
) {
    public ContestKoreaCrawlerRunResult {
        itemResults = itemResults == null ? List.of() : List.copyOf(itemResults);
    }
}
