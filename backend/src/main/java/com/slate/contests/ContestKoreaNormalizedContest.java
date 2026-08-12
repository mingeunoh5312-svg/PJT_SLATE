package com.slate.contests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.springframework.http.HttpStatus;

public record ContestKoreaNormalizedContest(
        String contestType,
        String title,
        String summary,
        String theme,
        String prizeText,
        Long totalPrizeAmount,
        Long firstPrizeAmount,
        String organizer,
        String organizerType,
        String representativeImageUrl,
        String posterSourceType,
        String posterOriginalUrl,
        LocalDateTime posterCollectedAt,
        String externalUrl,
        String targetText,
        List<String> targetCodes,
        List<String> regionCodes,
        String requiredRolesText,
        String relatedGenresText,
        LocalDateTime startAt,
        LocalDateTime deadlineAt,
        String status,
        String sourceName,
        String sourceExternalId,
        String sourceUrl,
        String sourceCategoryCode,
        LocalDateTime sourceCollectedAt,
        LocalDateTime sourceUpdatedAt,
        String sourcePermissionText,
        String sourceAttribution
) {
    private static final DateTimeFormatter ROW_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ContestKoreaNormalizedContest {
        targetCodes = targetCodes == null ? List.of() : List.copyOf(targetCodes);
        regionCodes = regionCodes == null ? List.of() : List.copyOf(regionCodes);
    }

    public Map<String, Object> toRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("contestType", contestType);
        row.put("title", title);
        row.put("summary", summary);
        row.put("theme", theme);
        row.put("prizeText", prizeText);
        row.put("totalPrizeAmount", totalPrizeAmount);
        row.put("firstPrizeAmount", firstPrizeAmount);
        row.put("organizer", organizer);
        row.put("organizerType", organizerType);
        row.put("representativeImageUrl", representativeImageUrl);
        row.put("representativeImagePath", null);
        row.put("posterSourceType", posterSourceType);
        row.put("posterOriginalUrl", posterOriginalUrl);
        row.put("posterCollectedAt", posterCollectedAt);
        row.put("submissionEmail", null);
        row.put("externalUrl", externalUrl);
        row.put("targetText", targetText);
        row.put("targetCodesJson", toJson(targetCodes));
        row.put("regionCodesJson", toJson(regionCodes));
        row.put("requiredRolesText", requiredRolesText);
        row.put("relatedGenresText", relatedGenresText);
        row.put("startAt", formatDateTime(startAt));
        row.put("deadlineAt", formatDateTime(deadlineAt));
        row.put("status", status);
        row.put("createdBy", null);
        row.put("requesterCompanyUserId", null);
        row.put("sourceRequestId", null);
        row.put("sourceName", sourceName);
        row.put("sourceExternalId", sourceExternalId);
        row.put("sourceUrl", sourceUrl);
        row.put("sourceCategoryCode", sourceCategoryCode);
        row.put("sourceCollectedAt", sourceCollectedAt);
        row.put("sourceUpdatedAt", sourceUpdatedAt);
        row.put("sourcePermissionText", sourcePermissionText);
        row.put("sourceAttribution", sourceAttribution);
        return row;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(ROW_DATE_TIME);
    }

    private String toJson(List<String> values) {
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "공모전 코드 JSON 변환 중 오류가 발생했습니다.");
        }
    }
}
