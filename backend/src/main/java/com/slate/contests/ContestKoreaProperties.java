package com.slate.contests;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "slate.public-data.contest-korea")
public record ContestKoreaProperties(
        Boolean enabled,
        String baseUrl,
        String listPath,
        String categoryCode,
        Integer intGbn,
        String userAgent,
        Long requestDelayMillis,
        Long connectTimeoutMillis,
        Long readTimeoutMillis,
        Integer maxPages,
        Integer maxItemsPerRun,
        Boolean posterDownloadEnabled,
        String requiredPermissionText,
        String sourceName,
        String sourceAttribution
) {
    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_BASE_URL = "https://www.contestkorea.com";
    private static final String DEFAULT_LIST_PATH = "/sub/list.php";
    private static final String DEFAULT_CATEGORY_CODE = "031210001";
    private static final int DEFAULT_INT_GBN = 1;
    private static final String DEFAULT_USER_AGENT = "SlateBot/1.0 (contact: helpdesk@slate.local)";
    private static final long DEFAULT_REQUEST_DELAY_MILLIS = 1500L;
    private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000L;
    private static final long DEFAULT_READ_TIMEOUT_MILLIS = 10000L;
    private static final int DEFAULT_MAX_PAGES = 10;
    private static final int DEFAULT_MAX_ITEMS_PER_RUN = 100;
    private static final boolean DEFAULT_POSTER_DOWNLOAD_ENABLED = true;
    private static final String DEFAULT_REQUIRED_PERMISSION_TEXT = "콘테스트코리아 출처 표기";
    private static final String DEFAULT_SOURCE_NAME = "CONTESTKOREA";
    private static final String DEFAULT_SOURCE_ATTRIBUTION = "출처: 콘테스트코리아";
    private static final int MAX_PAGES_UPPER_BOUND = 10;
    private static final int MAX_ITEMS_PER_RUN_UPPER_BOUND = 100;

    public ContestKoreaProperties {
        enabled = enabled == null ? DEFAULT_ENABLED : enabled;
        baseUrl = normalizeBaseUrl(baseUrl);
        listPath = normalizeListPath(listPath);
        categoryCode = requiredText(categoryCode, DEFAULT_CATEGORY_CODE, "categoryCode");
        intGbn = positiveInt(intGbn, DEFAULT_INT_GBN, "intGbn");
        userAgent = requiredText(userAgent, DEFAULT_USER_AGENT, "userAgent");
        requestDelayMillis = minimumLong(requestDelayMillis, DEFAULT_REQUEST_DELAY_MILLIS, 1000L, "requestDelayMillis");
        connectTimeoutMillis = positiveLong(connectTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS, "connectTimeoutMillis");
        readTimeoutMillis = positiveLong(readTimeoutMillis, DEFAULT_READ_TIMEOUT_MILLIS, "readTimeoutMillis");
        maxPages = boundedInt(maxPages, DEFAULT_MAX_PAGES, 1, MAX_PAGES_UPPER_BOUND, "maxPages");
        maxItemsPerRun = boundedInt(maxItemsPerRun, DEFAULT_MAX_ITEMS_PER_RUN, 1, MAX_ITEMS_PER_RUN_UPPER_BOUND, "maxItemsPerRun");
        posterDownloadEnabled = posterDownloadEnabled == null ? DEFAULT_POSTER_DOWNLOAD_ENABLED : posterDownloadEnabled;
        requiredPermissionText = requiredText(requiredPermissionText, DEFAULT_REQUIRED_PERMISSION_TEXT, "requiredPermissionText");
        sourceName = requiredText(sourceName, DEFAULT_SOURCE_NAME, "sourceName");
        sourceAttribution = requiredText(sourceAttribution, DEFAULT_SOURCE_ATTRIBUTION, "sourceAttribution");
    }

    public URI baseUri() {
        return URI.create(baseUrl);
    }

    public URI listUri() {
        return baseUri().resolve(listPath);
    }

    public Map<String, String> listQueryParams() {
        return Map.of(
                "Txt_bcode", categoryCode,
                "int_gbn", String.valueOf(intGbn)
        );
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = StringUtils.hasText(value) ? trimTrailingSlash(value.trim()) : DEFAULT_BASE_URL;
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("baseUrl must be a valid HTTP or HTTPS URL.", ex);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("baseUrl must use http or https.");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("baseUrl must include a host.");
        }
        return normalized;
    }

    private static String normalizeListPath(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim() : DEFAULT_LIST_PATH;
        if (!normalized.startsWith("/")) {
            throw new IllegalArgumentException("listPath must start with '/'.");
        }
        return normalized;
    }

    private static String requiredText(String value, String defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private static Integer positiveInt(Integer value, int defaultValue, String fieldName) {
        int normalized = value == null ? defaultValue : value;
        if (normalized <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
        return normalized;
    }

    private static Long positiveLong(Long value, long defaultValue, String fieldName) {
        long normalized = value == null ? defaultValue : value;
        if (normalized <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
        return normalized;
    }

    private static Long minimumLong(Long value, long defaultValue, long minimum, String fieldName) {
        long normalized = value == null ? defaultValue : value;
        if (normalized < minimum) {
            throw new IllegalArgumentException(fieldName + " must be at least " + minimum + ".");
        }
        return normalized;
    }

    private static Integer boundedInt(Integer value, int defaultValue, int minimum, int maximum, String fieldName) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < minimum || normalized > maximum) {
            throw new IllegalArgumentException(fieldName + " must be between " + minimum + " and " + maximum + ".");
        }
        return normalized;
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
