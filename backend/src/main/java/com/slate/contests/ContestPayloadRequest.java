package com.slate.contests;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ContestPayloadRequest(
        String contestType,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 500) String summary,
        @Size(max = 150) String theme,
        @Size(max = 150) String prizeText,
        @PositiveOrZero Long totalPrizeAmount,
        @PositiveOrZero Long firstPrizeAmount,
        @NotBlank @Size(max = 120) String organizer,
        @Size(max = 50) String organizerType,
        @Size(max = 500) String representativeImageUrl,
        @Size(max = 255) String submissionEmail,
        @Size(max = 500) String externalUrl,
        @Size(max = 500) String targetText,
        @Size(max = 10) List<@Size(max = 40) String> targetCodes,
        @Size(max = 24) List<@Size(max = 40) String> regionCodes,
        @Size(max = 500) String requiredRolesText,
        @Size(max = 500) String relatedGenresText,
        String startAt,
        @NotBlank String deadlineAt
) {
}
