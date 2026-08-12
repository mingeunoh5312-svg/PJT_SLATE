package com.slate.contests;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slate.common.SlateException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContestKoreaDataNormalizer {

    private static final String CONTEST_TYPE_EXTERNAL = "EXTERNAL";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ENDED = "ENDED";
    private static final String POSTER_ALLOWED = "CONTESTKOREA_ALLOWED";
    private static final String POSTER_NONE = "NONE";
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "([0-9][0-9,]*)\\s*(억|천\\s*만\\s*원?|천만원|천만|만\\s*원|만원|만|원)"
    );
    private static final Pattern TOTAL_PRIZE_LABEL = Pattern.compile("총\\s*(?:상금|시상금|규모)|시상\\s*규모");
    private static final Pattern FIRST_PRIZE_LABEL = Pattern.compile("1\\s*(?:위|등)|일\\s*(?:위|등)|대상|최우수상");
    private static final List<KeywordMapping> TARGET_MAPPINGS = List.of(
            mapping("ANYONE", "누구나", "전체", "전국민"),
            mapping("PRESCHOOL", "유치원"),
            mapping("ELEMENTARY", "초등학생", "초등"),
            mapping("MIDDLE", "중학생", "중등"),
            mapping("HIGH", "고등학생", "고등"),
            mapping("UNIVERSITY", "대학생", "대학교"),
            mapping("GRADUATE", "대학원생", "대학원"),
            mapping("ADULT", "일반인", "성인"),
            mapping("FOREIGNER", "외국인"),
            mapping("ELIGIBLE_ONLY", "해당자", "제한", "자격")
    );
    private static final List<KeywordMapping> REGION_MAPPINGS = List.of(
            mapping("ONLINE", "온라인"),
            mapping("CAPITAL", "수도권"),
            mapping("NATIONWIDE", "전국", "전 국"),
            mapping("SEOUL", "서울"),
            mapping("INCHEON", "인천"),
            mapping("DAEJEON", "대전"),
            mapping("GWANGJU", "광주"),
            mapping("DAEGU", "대구"),
            mapping("BUSAN", "부산"),
            mapping("ULSAN", "울산"),
            mapping("SEJONG", "세종"),
            mapping("GYEONGGI", "경기"),
            mapping("GANGWON", "강원"),
            mapping("CHUNGNAM", "충남"),
            mapping("CHUNGBUK", "충북"),
            mapping("JEONNAM", "전남"),
            mapping("JEONBUK", "전북"),
            mapping("GYEONGNAM", "경남"),
            mapping("GYEONGBUK", "경북"),
            mapping("JEJU", "제주"),
            mapping("OVERSEAS", "해외"),
            mapping("OTHER", "기타")
    );
    private static final List<KeywordMapping> ORGANIZER_TYPE_MAPPINGS = List.of(
            mapping("GOVERNMENT_PUBLIC", "정부", "지자체", "공공기관", "시청", "구청", "도청", "교육청", "서울시"),
            mapping("MEDIA_PUBLISHER", "신문", "방송", "언론", "출판"),
            mapping("SCHOOL_ASSOCIATION", "학교", "대학교", "학회", "협회", "재단"),
            mapping("CULTURE_VENUE", "미술관", "박물관", "전시", "공연장"),
            mapping("COMPANY", "기업", "주식회사", "(주)", "그룹", "벤처", "스타트업"),
            mapping("ORGANIZATION", "센터", "위원회", "협의회", "연구회", "단체"),
            mapping("CLUB", "동아리", "모임"),
            mapping("OVERSEAS", "해외", "외국")
    );

    private final Clock clock;

    public ContestKoreaDataNormalizer() {
        this(Clock.systemDefaultZone());
    }

    ContestKoreaDataNormalizer(Clock clock) {
        this.clock = clock;
    }

    public ContestKoreaNormalizedContest normalize(ContestKoreaParsedContest parsed) {
        if (parsed == null) {
            throw new SlateException("콘테스트코리아 파싱 결과가 필요합니다.");
        }
        String contestType = normalizeContestType(parsed.contestType());
        LocalDateTime deadlineAt = requireDateTime(parsed.deadlineAt(), "마감일");
        LocalDateTime startAt = parsed.startAt();
        if (startAt != null && startAt.isAfter(deadlineAt)) {
            throw new SlateException("시작일은 마감일보다 늦을 수 없습니다.");
        }

        String sourceUrl = requireHttpUrl(parsed.sourceUrl(), 500, "sourceUrl");
        String title = requireText(parsed.title(), 200, "제목");
        String summary = requireText(parsed.summary(), 500, "요약");
        String theme = truncate(parsed.theme(), 150);
        String prizeText = truncate(parsed.prizeText(), 150);
        PrizeAmounts prizeAmounts = normalizePrizeAmounts(prizeText, parsed.totalPrizeAmount(), parsed.firstPrizeAmount());
        String organizer = requireText(cleanOrganizer(parsed.organizer()), 120, "주최");
        String organizerType = normalizeOrganizerType(parsed.organizerType(), organizer, theme);
        String sourceName = requireText(parsed.sourceName(), 80, "sourceName");
        String sourceExternalId = requireText(parsed.sourceExternalId(), 100, "sourceExternalId");
        String sourcePermissionText = truncate(parsed.sourcePermissionText(), 1000);
        String posterSourceType = normalizePosterSourceType(parsed.posterSourceType(), sourcePermissionText);
        String posterOriginalUrl = POSTER_ALLOWED.equals(posterSourceType) ? optionalHttpUrl(parsed.posterOriginalUrl(), 500) : null;
        LocalDateTime sourceCollectedAt = parsed.sourceCollectedAt() == null ? LocalDateTime.now(clock) : parsed.sourceCollectedAt();
        LocalDateTime sourceUpdatedAt = parsed.sourceUpdatedAt() == null ? sourceCollectedAt : parsed.sourceUpdatedAt();

        return new ContestKoreaNormalizedContest(
                contestType,
                title,
                summary,
                theme,
                prizeText,
                prizeAmounts.totalPrizeAmount(),
                prizeAmounts.firstPrizeAmount(),
                organizer,
                organizerType,
                null,
                posterSourceType,
                posterOriginalUrl,
                posterOriginalUrl == null ? null : parsed.posterCollectedAt(),
                normalizeExternalUrl(parsed.externalUrl(), sourceUrl),
                truncate(parsed.targetText(), 500),
                targetCodes(parsed.targetText()),
                regionCodes(parsed.regionText()),
                truncate(parsed.requiredRolesText(), 500),
                truncate(parsed.relatedGenresText(), 500),
                startAt,
                deadlineAt,
                normalizeStatus(parsed.status(), deadlineAt),
                sourceName,
                sourceExternalId,
                sourceUrl,
                truncate(parsed.sourceCategoryCode(), 50),
                sourceCollectedAt,
                sourceUpdatedAt,
                sourcePermissionText,
                truncate(parsed.sourceAttribution(), 120)
        );
    }

    private String normalizeContestType(String value) {
        if (!StringUtils.hasText(value)) {
            return CONTEST_TYPE_EXTERNAL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!CONTEST_TYPE_EXTERNAL.equals(normalized)) {
            throw new SlateException("콘테스트코리아 공모전 유형은 EXTERNAL이어야 합니다.");
        }
        return normalized;
    }

    private String normalizeStatus(String status, LocalDateTime deadlineAt) {
        String parsedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : STATUS_OPEN;
        if (!STATUS_OPEN.equals(parsedStatus) && !STATUS_ENDED.equals(parsedStatus)) {
            throw new SlateException("콘테스트코리아 공모전 상태는 OPEN 또는 ENDED만 가능합니다.");
        }
        if (STATUS_ENDED.equals(parsedStatus)) {
            return STATUS_ENDED;
        }
        return deadlineAt.isBefore(LocalDateTime.now(clock)) ? STATUS_ENDED : STATUS_OPEN;
    }

    private List<String> targetCodes(String targetText) {
        List<String> codes = codesByKeyword(targetText, TARGET_MAPPINGS, ContestFilterCatalog.TARGETS);
        return codes.isEmpty() ? List.of("ANYONE") : codes;
    }

    private List<String> regionCodes(String regionText) {
        if (!StringUtils.hasText(regionText)) {
            return List.of("ALL");
        }
        List<String> codes = codesByKeyword(regionText, REGION_MAPPINGS, ContestFilterCatalog.REGIONS);
        return codes.isEmpty() ? List.of("OTHER") : codes;
    }

    private List<String> codesByKeyword(String value, List<KeywordMapping> mappings, Set<String> allowed) {
        String text = normalizeText(value);
        String compact = compact(text);
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (KeywordMapping mapping : mappings) {
            if (!allowed.contains(mapping.code())) {
                continue;
            }
            if (mapping.keywords().stream().anyMatch(keyword -> containsKeyword(text, compact, keyword))) {
                codes.add(mapping.code());
            }
        }
        return new ArrayList<>(codes);
    }

    private String normalizeOrganizerType(String parsedType, String organizer, String theme) {
        String normalized = normalizeCode(parsedType, ContestFilterCatalog.ORGANIZER_TYPES, "주최 유형");
        if (normalized != null) {
            return normalized;
        }
        List<String> inferred = codesByKeyword(organizer + " " + Objects.toString(theme, ""), ORGANIZER_TYPE_MAPPINGS, ContestFilterCatalog.ORGANIZER_TYPES);
        return inferred.isEmpty() ? "OTHER" : inferred.get(0);
    }

    private PrizeAmounts normalizePrizeAmounts(String prizeText, Long parsedTotal, Long parsedFirst) {
        Long total = nonNegative(parsedTotal, "총상금");
        Long first = nonNegative(parsedFirst, "1등 상금");
        PrizeAmounts extracted = extractPrizeAmounts(prizeText);
        if (invalidPrizePair(total, first) && validPrizePair(extracted.totalPrizeAmount(), extracted.firstPrizeAmount())) {
            total = extracted.totalPrizeAmount();
            first = extracted.firstPrizeAmount();
        }
        if (total == null) {
            total = extracted.totalPrizeAmount();
        }
        if (first == null) {
            first = extracted.firstPrizeAmount();
        }
        if (invalidPrizePair(total, first)) {
            return new PrizeAmounts(Math.max(total, first), Math.min(total, first));
        }
        return new PrizeAmounts(total, first);
    }

    private boolean validPrizePair(Long total, Long first) {
        return total != null && first != null && first <= total;
    }

    private boolean invalidPrizePair(Long total, Long first) {
        return total != null && first != null && first > total;
    }

    private PrizeAmounts extractPrizeAmounts(String prizeText) {
        if (!StringUtils.hasText(prizeText)) {
            return new PrizeAmounts(null, null);
        }
        Long total = firstAmountAfter(TOTAL_PRIZE_LABEL, prizeText);
        Long first = firstAmountAfter(FIRST_PRIZE_LABEL, prizeText);
        List<Long> amounts = allAmounts(prizeText);
        if (total == null && !amounts.isEmpty()) {
            total = amounts.get(0);
        }
        if (first == null && amounts.size() >= 2) {
            first = amounts.get(1);
        }
        return new PrizeAmounts(total, first);
    }

    private Long firstAmountAfter(Pattern label, String value) {
        Matcher labelMatcher = label.matcher(value);
        if (!labelMatcher.find()) {
            return null;
        }
        String tail = value.substring(labelMatcher.end(), Math.min(value.length(), labelMatcher.end() + 80));
        List<Long> amounts = allAmounts(tail);
        return amounts.isEmpty() ? null : amounts.get(0);
    }

    private List<Long> allAmounts(String value) {
        List<Long> amounts = new ArrayList<>();
        Matcher matcher = MONEY_PATTERN.matcher(value);
        while (matcher.find()) {
            Long amount = moneyAmount(value, matcher);
            if (amount != null && amount > 0) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    private Long moneyAmount(String raw, Matcher matcher) {
        int previous = matcher.start() - 1;
        while (previous >= 0 && Character.isWhitespace(raw.charAt(previous))) {
            previous--;
        }
        if (previous >= 0 && (raw.charAt(previous) == '-' || raw.charAt(previous) == '−')) {
            return null;
        }
        long value = Long.parseLong(matcher.group(1).replace(",", ""));
        String unit = matcher.group(2).replaceAll("\\s+", "");
        if ("억".equals(unit)) {
            return value * 100_000_000L;
        }
        if (unit.startsWith("천만")) {
            return value * 10_000_000L;
        }
        if (unit.startsWith("만")) {
            return value * 10_000L;
        }
        return value;
    }

    private Long nonNegative(Long value, String label) {
        if (value != null && value < 0) {
            throw new SlateException(label + "은 0 이상이어야 합니다.");
        }
        return value;
    }

    private String normalizePosterSourceType(String value, String sourcePermissionText) {
        if (!StringUtils.hasText(sourcePermissionText)) {
            return POSTER_NONE;
        }
        return POSTER_ALLOWED.equals(value.trim().toUpperCase(Locale.ROOT)) ? POSTER_ALLOWED : POSTER_NONE;
    }

    private String normalizeExternalUrl(String value, String sourceUrl) {
        String externalUrl = optionalHttpUrl(value, 500);
        if (externalUrl == null || sameUrl(externalUrl, sourceUrl) || isContestKoreaUrl(externalUrl)) {
            return null;
        }
        return externalUrl;
    }

    private String requireHttpUrl(String value, int maxLength, String label) {
        String normalized = optionalHttpUrl(value, maxLength);
        if (normalized == null) {
            throw new SlateException(label + "은 HTTP 또는 HTTPS 절대 URL이어야 합니다.");
        }
        return normalized;
    }

    private String optionalHttpUrl(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            if (!StringUtils.hasText(uri.getHost())) {
                return null;
            }
            String normalized = uri.toString();
            return normalized.length() <= maxLength ? normalized : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean sameUrl(String first, String second) {
        URI firstUri = safeUri(first);
        URI secondUri = safeUri(second);
        if (firstUri == null || secondUri == null) {
            return false;
        }
        return Objects.equals(normalizeScheme(firstUri), normalizeScheme(secondUri))
                && Objects.equals(normalizeHost(firstUri), normalizeHost(secondUri))
                && normalizedPort(firstUri) == normalizedPort(secondUri)
                && Objects.equals(Objects.toString(firstUri.getPath(), ""), Objects.toString(secondUri.getPath(), ""))
                && Objects.equals(Objects.toString(firstUri.getQuery(), ""), Objects.toString(secondUri.getQuery(), ""));
    }

    private boolean isContestKoreaUrl(String value) {
        URI uri = safeUri(value);
        if (uri == null) {
            return false;
        }
        String host = normalizeHost(uri);
        return "contestkorea.com".equals(host) || host.endsWith(".contestkorea.com");
    }

    private URI safeUri(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeScheme(URI uri) {
        return Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
    }

    private String normalizeHost(URI uri) {
        String host = Objects.toString(uri.getHost(), "").toLowerCase(Locale.ROOT);
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private int normalizedPort(URI uri) {
        int port = uri.getPort();
        if (port >= 0) {
            return port;
        }
        String scheme = normalizeScheme(uri);
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        return -1;
    }

    private String normalizeCode(String value, Set<String> allowed, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SlateException(label + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private LocalDateTime requireDateTime(LocalDateTime value, String label) {
        if (value == null) {
            throw new SlateException(label + "은 필수입니다.");
        }
        return value;
    }

    private String requireText(String value, int maxLength, String label) {
        String normalized = truncate(value, maxLength);
        if (!StringUtils.hasText(normalized)) {
            throw new SlateException(label + "은 필수입니다.");
        }
        return normalized;
    }

    private String cleanOrganizer(String value) {
        String normalized = normalizeText(value);
        normalized = normalized.replaceFirst("^(주최\\s*/\\s*주관|주최\\s*·\\s*주관|주최|주관)\\s*[:：ㆍ·|/-]*\\s*", "");
        normalized = normalized.replaceAll("\\s*([,/|])\\s*", "$1 ");
        normalized = normalized.replaceAll("([,/|]\\s*){2,}", "$1");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceAll("[,/|]\\s*$", "").trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private boolean containsKeyword(String text, String compact, String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        return text.contains(normalizedKeyword) || compact.contains(compact(normalizedKeyword));
    }

    private String compact(String value) {
        return normalizeText(value).replaceAll("[\\s,./·ㆍ•:：-]+", "");
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00a0', ' ')
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static KeywordMapping mapping(String code, String... keywords) {
        return new KeywordMapping(code, List.of(keywords));
    }

    private record KeywordMapping(String code, List<String> keywords) { }

    private record PrizeAmounts(Long totalPrizeAmount, Long firstPrizeAmount) { }
}
