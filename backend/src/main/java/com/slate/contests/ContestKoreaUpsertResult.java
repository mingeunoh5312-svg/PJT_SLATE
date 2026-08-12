package com.slate.contests;

public record ContestKoreaUpsertResult(
        Long contestId,
        String sourceName,
        String sourceExternalId,
        boolean inserted,
        boolean updated,
        boolean posterStored,
        String representativeImagePath
) {
}
