package com.slate.contests;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slate.common.SlateException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ContestKoreaParser {

    private static final String DETAIL_PATH = "/sub/view.php";
    private static final String ACCEPTED_CATEGORY = "사진영상영화제";
    private static final List<String> KNOWN_NON_TARGET_CATEGORIES = List.of("문학", "광고", "디자인", "음악", "과학", "게임");
    private static final Pattern DATE_RANGE_SEPARATOR = Pattern.compile("\\s*(?:~|–|-)\\s*");
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile(
            "(?:(\\d{2,4})\\s*년\\s*)?(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일?\\s*(?:(\\d{1,2})\\s*(?::|시)\\s*(\\d{1,2})?\\s*(?:분)?)?"
    );
    private static final Pattern DOT_DATE_PATTERN = Pattern.compile(
            "(?:(\\d{4})[.\\-/]\\s*)?(\\d{1,2})[.\\-/]\\s*(\\d{1,2})(?:\\s+(\\d{1,2}):(\\d{1,2}))?"
    );
    private static final Pattern MONEY_PATTERN = Pattern.compile("([0-9][0-9,]*)\\s*(억|천만|만원|만|원)?");
    private static final Pattern DOWNLOAD_FILE_PATTERN = Pattern.compile(
            ".*\\.(?:pdf|hwp|hwpx|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z)(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> OFFICIAL_LINK_KEYWORDS = List.of(
            "접수", "신청", "공식", "공고", "홈페이지", "바로가기", "자세히", "주최", "사이트", "참가", "지원"
    );
    private static final List<String> NON_OFFICIAL_LINK_KEYWORDS = List.of(
            "공유", "sns", "페이스북", "facebook", "인스타", "instagram", "카카오", "트위터", "twitter",
            "x.com", "블로그", "youtube", "유튜브", "광고", "배너", "다운로드", "첨부", "파일"
    );
    private static final List<String> NON_OFFICIAL_HOST_PARTS = List.of(
            "facebook.com", "instagram.com", "twitter.com", "x.com", "youtube.com", "youtu.be",
            "kakao.com", "story.kakao.com", "band.us", "naver.me"
    );

    private final ContestKoreaProperties properties;
    private final Clock clock;

    @Autowired
    public ContestKoreaParser(ContestKoreaProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    ContestKoreaParser(ContestKoreaProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public List<ContestKoreaListItem> parseList(String html) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }
        Document document = Jsoup.parse(html, properties.baseUrl());
        Map<String, ContestKoreaListItem> items = new LinkedHashMap<>();
        for (Element link : document.select("a[href]")) {
            URI detailUri = normalizeDetailUrl(link.attr("href"));
            if (detailUri == null) {
                continue;
            }
            String sourceExternalId = queryParam(detailUri, "str_no");
            if (!StringUtils.hasText(sourceExternalId) || items.containsKey(sourceExternalId)) {
                continue;
            }
            Element container = listContainer(link);
            if (!categoryAllowed(container)) {
                continue;
            }
            items.put(sourceExternalId, new ContestKoreaListItem(
                    truncate(textOrNull(link.text()), 200),
                    truncate(valueByLabels(container, "주최", "주최/주관"), 120),
                    truncate(valueByLabels(container, "대상", "참가대상"), 500),
                    truncate(valueByLabels(container, "상태", "접수상태", "D-day"), 120),
                    truncate(valueByLabels(container, "접수", "접수기간", "기간"), 120),
                    detailUri.toString(),
                    sourceExternalId,
                    properties.categoryCode()
            ));
        }
        return List.copyOf(items.values());
    }

    public ContestKoreaParsedContest parseDetail(String html, String detailUrl) {
        if (!StringUtils.hasText(html)) {
            throw new SlateException("콘테스트코리아 상세 HTML이 비어 있습니다.");
        }
        URI sourceUri = requireDetailUrl(detailUrl);
        String sourceExternalId = queryParam(sourceUri, "str_no");
        if (!StringUtils.hasText(sourceExternalId)) {
            throw new SlateException("콘테스트코리아 상세 URL에서 str_no를 찾을 수 없습니다.");
        }

        LocalDateTime collectedAt = LocalDateTime.now(clock);
        Document document = Jsoup.parse(html, sourceUri.toString());
        Element detailRoot = detailRoot(document);
        String title = truncate(firstText(detailRoot, ".view_top_area h1, h1.title, h1, .view-title, .contest-title"), 200);
        if (!StringUtils.hasText(title)) {
            title = truncate(metaContent(document, "meta[property=og:title], meta[name=twitter:title]"), 200);
        }
        if (!StringUtils.hasText(title)) {
            title = truncate(textOrNull(document.title()), 200);
        }
        if (!StringUtils.hasText(title)) {
            throw new SlateException("콘테스트코리아 상세 제목을 찾을 수 없습니다.");
        }

        String organizer = truncate(valueByLabels(detailRoot, "주최", "주최/주관", "주최/주관사"), 120);
        if (!StringUtils.hasText(organizer)) {
            organizer = "콘테스트코리아";
        }
        String theme = truncate(valueByLabels(detailRoot, "대표분야", "분야"), 150);
        String targetText = truncate(valueByLabels(detailRoot, "참가대상", "대상"), 500);
        String regionText = truncate(valueByLabels(detailRoot, "대회지역", "지역"), 120);
        String prizeText = truncate(valueByLabels(detailRoot, "시상내역", "시상", "상금"), 150);
        String periodText = valueByLabels(detailRoot, "접수기간", "접수");
        if (!StringUtils.hasText(periodText)) {
            periodText = firstMatchingText(document, DATE_RANGE_SEPARATOR);
        }
        DateRange dateRange = parseDateRange(periodText, collectedAt);

        String bodyText = bodyText(detailRoot, document);
        String summary = truncate(StringUtils.hasText(bodyText) ? bodyText : title, 500);
        List<Long> prizeAmounts = parsePrizeAmounts(prizeText);
        String permissionText = permissionText(bodyText);
        String posterOriginalUrl = null;
        LocalDateTime posterCollectedAt = null;
        String posterSourceType = "NONE";
        if (permissionText != null) {
            posterSourceType = "CONTESTKOREA_ALLOWED";
            if (Boolean.TRUE.equals(properties.posterDownloadEnabled())) {
                posterOriginalUrl = firstSafeImageUrl(detailRoot, document);
                posterCollectedAt = posterOriginalUrl == null ? null : collectedAt;
            }
        }

        return new ContestKoreaParsedContest(
                "EXTERNAL",
                "OPEN",
                title,
                summary,
                theme,
                prizeText,
                prizeAmounts.isEmpty() ? null : prizeAmounts.get(0),
                prizeAmounts.size() >= 2 ? prizeAmounts.get(1) : null,
                organizer,
                null,
                null,
                targetText,
                regionText,
                null,
                null,
                dateRange.startAt(),
                dateRange.deadlineAt(),
                firstExternalUrl(detailRoot),
                properties.sourceName(),
                sourceExternalId,
                sourceUri.toString(),
                properties.categoryCode(),
                collectedAt,
                collectedAt,
                permissionText,
                properties.sourceAttribution(),
                posterSourceType,
                posterOriginalUrl,
                posterCollectedAt
        );
    }

    private DateRange parseDateRange(String value, LocalDateTime collectedAt) {
        if (!StringUtils.hasText(value)) {
            throw new SlateException("콘테스트코리아 상세 접수기간을 해석할 수 없습니다.");
        }
        String normalized = normalizeText(value).replaceAll("\\([^)]*\\)", " ");
        String[] parts = DATE_RANGE_SEPARATOR.split(normalized, 2);
        if (parts.length < 2) {
            throw new SlateException("콘테스트코리아 상세 접수기간을 해석할 수 없습니다.");
        }
        LocalDateTime startAt = parseDateTime(parts[0], false, collectedAt);
        LocalDateTime deadlineAt = parseDateTime(parts[1], true, collectedAt);
        if (startAt == null || deadlineAt == null) {
            throw new SlateException("콘테스트코리아 상세 접수기간을 해석할 수 없습니다.");
        }
        if (deadlineAt.isBefore(startAt) && !hasExplicitYear(parts[1])) {
            deadlineAt = deadlineAt.plusYears(1);
        }
        if (deadlineAt.isBefore(startAt)) {
            throw new SlateException("콘테스트코리아 상세 접수기간이 올바르지 않습니다.");
        }
        return new DateRange(startAt, deadlineAt);
    }

    private LocalDateTime parseDateTime(String raw, boolean endOfDay, LocalDateTime collectedAt) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = normalizeText(raw).replaceAll("\\([^)]*\\)", " ");
        Matcher korean = KOREAN_DATE_PATTERN.matcher(value);
        if (korean.find()) {
            return dateTime(
                    korean.group(1),
                    korean.group(2),
                    korean.group(3),
                    korean.group(4),
                    korean.group(5),
                    endOfDay,
                    collectedAt
            );
        }
        Matcher dot = DOT_DATE_PATTERN.matcher(value);
        if (dot.find()) {
            return dateTime(
                    dot.group(1),
                    dot.group(2),
                    dot.group(3),
                    dot.group(4),
                    dot.group(5),
                    endOfDay,
                    collectedAt
            );
        }
        return null;
    }

    private LocalDateTime dateTime(
            String yearText,
            String monthText,
            String dayText,
            String hourText,
            String minuteText,
            boolean endOfDay,
            LocalDateTime collectedAt
    ) {
        int year = StringUtils.hasText(yearText) ? Integer.parseInt(yearText) : collectedAt.getYear();
        if (year < 100) {
            year += 2000;
        }
        int month = Integer.parseInt(monthText);
        int day = Integer.parseInt(dayText);
        LocalTime time;
        if (StringUtils.hasText(hourText)) {
            time = LocalTime.of(Integer.parseInt(hourText), StringUtils.hasText(minuteText) ? Integer.parseInt(minuteText) : 0);
        } else {
            time = endOfDay ? LocalTime.of(23, 59, 59) : LocalTime.MIDNIGHT;
        }
        LocalDateTime parsed = LocalDateTime.of(LocalDate.of(year, month, day), time);
        if (!StringUtils.hasText(yearText) && endOfDay && parsed.isBefore(collectedAt.minusDays(30))) {
            parsed = parsed.plusYears(1);
        }
        return parsed;
    }

    private boolean hasExplicitYear(String value) {
        return Pattern.compile("\\d{2,4}\\s*년|\\d{4}[.\\-/]").matcher(value).find();
    }

    private URI requireDetailUrl(String value) {
        URI uri = normalizeDetailUrl(value);
        if (uri == null) {
            throw new SlateException("콘테스트코리아 상세 URL 형식이 올바르지 않습니다.");
        }
        return uri;
    }

    private URI normalizeDetailUrl(String value) {
        if (!StringUtils.hasText(value) || value.trim().startsWith("//")) {
            return null;
        }
        String raw = value.trim();
        URI uri;
        try {
            URI parsed = URI.create(raw);
            if (StringUtils.hasText(parsed.getScheme())) {
                uri = parsed;
            } else {
                uri = properties.baseUri().resolve(URI.create(normalizeRelativeDetailPath(raw)));
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        if (!Objects.toString(properties.baseUri().getHost(), "").equalsIgnoreCase(Objects.toString(uri.getHost(), ""))) {
            return null;
        }
        if (!DETAIL_PATH.equals(uri.getPath())) {
            return null;
        }
        if (!StringUtils.hasText(queryParam(uri, "str_no"))) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).fragment(null).build(true).toUri();
    }

    private String normalizeRelativeDetailPath(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        if (value.startsWith("sub/view.php")) {
            return "/" + value;
        }
        if (value.startsWith("view.php")) {
            return "/sub/" + value;
        }
        throw new IllegalArgumentException("Unsupported detail path.");
    }

    private String queryParam(URI uri, String key) {
        return UriComponentsBuilder.fromUri(uri).build(true).getQueryParams().getFirst(key);
    }

    private Element listContainer(Element link) {
        Element container = link.closest("li, tr, article");
        if (container == null) {
            container = link.closest("div");
        }
        return container == null ? link : container;
    }

    private boolean categoryAllowed(Element container) {
        String text = categoryText(container);
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = compactCategory(text);
        if (normalized.contains(ACCEPTED_CATEGORY)) {
            return true;
        }
        return KNOWN_NON_TARGET_CATEGORIES.stream().noneMatch(normalized::contains);
    }

    private String categoryText(Element container) {
        String explicit = firstText(container, ".category, .field, .contest-category, [data-field=category]");
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        return valueByLabels(container, "분야", "대표분야");
    }

    private String compactCategory(String value) {
        return normalizeText(value).replaceAll("[\\s•·/.,]+", "");
    }

    private String valueByLabels(Element root, String... labels) {
        if (root == null) {
            return null;
        }
        for (String label : labels) {
            String value = valueFromDefinitionList(root, label);
            if (StringUtils.hasText(value)) {
                return value;
            }
            value = valueFromRows(root, label);
            if (StringUtils.hasText(value)) {
                return value;
            }
            value = valueFromLabeledText(root, label);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String valueFromDefinitionList(Element root, String label) {
        for (Element dt : root.select("dt, th, .label, .tit")) {
            if (labelMatches(dt.text(), label)) {
                Element next = dt.nextElementSibling();
                if (next != null) {
                    return truncateByLabel(cleanLabel(next.text(), label), 500);
                }
            }
        }
        return null;
    }

    private String valueFromRows(Element root, String label) {
        for (Element row : root.select("tr, li, .info-row")) {
            Elements children = row.children();
            if (children.size() >= 2 && labelMatches(children.get(0).text(), label)) {
                return truncateByLabel(cleanLabel(children.get(1).text(), label), 500);
            }
        }
        return null;
    }

    private String valueFromLabeledText(Element root, String label) {
        Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s*(?:[.:：ㆍ·]|\\s)\\s*([^\\n|]+)");
        for (Element element : root.select("li, p, div, span")) {
            String ownText = normalizeText(element.ownText());
            Matcher matcher = pattern.matcher(ownText);
            if (matcher.find()) {
                return truncateByLabel(cleanLabel(matcher.group(1), label), 500);
            }
        }
        Matcher matcher = pattern.matcher(normalizeText(root.text()).replace("  ", "\n"));
        return matcher.find() ? truncateByLabel(cleanLabel(matcher.group(1), label), 500) : null;
    }

    private boolean labelMatches(String value, String label) {
        String normalized = normalizeText(value).replaceAll("[\\s.:：ㆍ·/]+", "");
        String normalizedLabel = label.replaceAll("[\\s.:：ㆍ·/]+", "");
        return normalized.equals(normalizedLabel) || normalized.startsWith(normalizedLabel);
    }

    private String cleanLabel(String value, String label) {
        String result = normalizeText(value);
        result = result.replaceFirst("^" + Pattern.quote(label) + "\\s*(?:[.:：ㆍ·]|\\s)*", "");
        result = result.replaceFirst("^(주최|주관|대상|접수|분야)\\s*[.:：ㆍ·]?\\s*", "");
        return textOrNull(result);
    }

    private String truncateByLabel(String value, int length) {
        return truncate(textOrNull(value), length);
    }

    private String firstText(Element root, String cssQuery) {
        Element element = root.selectFirst(cssQuery);
        return element == null ? null : textOrNull(element.text());
    }

    private String firstMatchingText(Document document, Pattern pattern) {
        for (Element element : document.select("li, p, div, td, dd")) {
            String text = normalizeText(element.text());
            if (pattern.matcher(text).find() && (text.contains(".") || text.contains("년") || text.contains("월"))) {
                return text;
            }
        }
        return null;
    }

    private Element detailRoot(Document document) {
        Element root = document.selectFirst(".view_cont_area, .contest-detail, article, .view_detail_area, .detail, .view, .content");
        return root == null ? document.body() : root;
    }

    private String bodyText(Element detailRoot, Document document) {
        Element body = detailRoot.selectFirst(".view_detail_area, .contest-detail, article, .detail, .content");
        return textOrNull(body == null ? detailRoot.text() : body.text());
    }

    private String permissionText(String bodyText) {
        if (!StringUtils.hasText(bodyText) || !bodyText.contains(properties.requiredPermissionText())) {
            return null;
        }
        for (String sentence : bodyText.split("(?<=[.!?。])\\s+|\\n+")) {
            if (sentence.contains(properties.requiredPermissionText())) {
                return permissionSnippet(sentence);
            }
        }
        return permissionSnippet(bodyText);
    }

    private String permissionSnippet(String value) {
        int index = value.indexOf(properties.requiredPermissionText());
        int start = Math.max(0, index - 120);
        int end = Math.min(value.length(), index + properties.requiredPermissionText().length() + 240);
        return truncate(textOrNull(value.substring(start, end)), 1000);
    }

    private String firstSafeImageUrl(Element detailRoot, Document document) {
        for (Element image : detailRoot.select(".view_top_area .img_area img[src], .view_detail_area .img_area img[src], .img_area img[src], article img[src], .contest-detail img[src]")) {
            String normalized = normalizeHttpUrl(image.absUrl("src"));
            if (normalized != null && isLikelyPosterImage(normalized)) {
                return normalized;
            }
        }
        String ogImage = normalizeHttpUrl(metaContent(document, "meta[property=og:image], meta[name=twitter:image]"));
        if (ogImage != null && isLikelyPosterImage(ogImage)) {
            return ogImage;
        }
        return null;
    }

    private boolean isLikelyPosterImage(String url) {
        if (!sameHost(url, properties.baseUri())) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.matches(".*\\.(jpg|jpeg|png|webp)(?:[?#].*)?$")) {
            return false;
        }
        return lower.contains("/admincenter/files/meet/")
                || lower.contains("/upload/")
                || lower.contains("/files/meet/")
                || lower.contains("/poster");
    }

    private String firstExternalUrl(Element root) {
        for (Element link : root.select("a[href]")) {
            String normalized = normalizeHttpUrl(link.absUrl("href"));
            if (!isOfficialExternalUrl(normalized) || !hasOfficialLinkContext(link)) {
                continue;
            }
            return normalized;
        }
        return null;
    }

    private boolean hasOfficialLinkContext(Element link) {
        String context = normalizeText(String.join(" ",
                link.text(),
                link.attr("title"),
                link.attr("aria-label"),
                link.attr("class"),
                link.attr("id"),
                link.parent() == null ? "" : link.parent().ownText()
        )).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(context) || NON_OFFICIAL_LINK_KEYWORDS.stream().anyMatch(context::contains)) {
            return false;
        }
        return OFFICIAL_LINK_KEYWORDS.stream().anyMatch(context::contains);
    }

    private boolean isOfficialExternalUrl(String url) {
        if (url == null || sameHost(url, properties.baseUri()) || DOWNLOAD_FILE_PATTERN.matcher(url).matches()) {
            return false;
        }
        String host = normalizedHost(url);
        if (!StringUtils.hasText(host)) {
            return false;
        }
        return NON_OFFICIAL_HOST_PARTS.stream().noneMatch(host::contains);
    }

    private String metaContent(Document document, String cssQuery) {
        Element element = document.selectFirst(cssQuery);
        return element == null ? null : textOrNull(element.attr("content"));
    }

    private String normalizeHttpUrl(String value) {
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
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean sameHost(String url, URI baseUri) {
        String urlHost = rootHost(normalizedHost(url));
        String baseHost = rootHost(baseUri.getHost());
        return StringUtils.hasText(urlHost) && (urlHost.equals(baseHost) || urlHost.endsWith("." + baseHost));
    }

    private String normalizedHost(String url) {
        try {
            URI uri = URI.create(url);
            return rootHost(uri.getHost());
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String rootHost(String host) {
        String normalized = Objects.toString(host, "").toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    private List<Long> parsePrizeAmounts(String prizeText) {
        if (!StringUtils.hasText(prizeText)) {
            return List.of();
        }
        List<Long> amounts = new ArrayList<>();
        Matcher matcher = MONEY_PATTERN.matcher(prizeText);
        while (matcher.find()) {
            Long amount = moneyAmount(matcher.group(1), matcher.group(2));
            if (amount != null && amount > 0) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    private Long moneyAmount(String numberText, String unit) {
        long value = Long.parseLong(numberText.replace(",", ""));
        if ("억".equals(unit)) return value * 100_000_000L;
        if ("천만".equals(unit)) return value * 10_000_000L;
        if ("만원".equals(unit) || "만".equals(unit)) return value * 10_000L;
        return value;
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

    private String textOrNull(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalizeText(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private record DateRange(LocalDateTime startAt, LocalDateTime deadlineAt) { }
}
