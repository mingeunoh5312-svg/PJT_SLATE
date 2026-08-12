package com.slate.contests;

public record ContestKoreaCrawlerRunRequest(
        Integer maxPages,
        Integer maxItems,
        Boolean dryRun
) {
}
