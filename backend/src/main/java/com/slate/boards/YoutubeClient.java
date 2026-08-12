package com.slate.boards;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class YoutubeClient {

    private static final String EMBED_BASE_URL = "https://www.youtube.com/embed/";
    private static final List<String> THUMBNAIL_PRIORITY = List.of("maxres", "standard", "high", "medium", "default");

    private final YoutubeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public YoutubeClient(YoutubeProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
        String originalUrl = cleanUrl(youtubeUrl);
        String videoId = extractVideoId(originalUrl);
        if (!properties.hasApiKey()) {
            throw new SlateException("YouTube API Key가 설정되어 있지 않습니다.");
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                    .path("/videos")
                    .queryParam("part", "snippet,contentDetails")
                    .queryParam("id", videoId)
                    .queryParam("key", properties.apiKey())
                    .encode()
                    .build()
                    .toUri();
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            JsonNode item = firstVideoItem(body);
            JsonNode snippet = item.path("snippet");
            JsonNode contentDetails = item.path("contentDetails");
            return new YoutubeVideoMetadata(
                    videoId,
                    embedUrl(videoId),
                    embedUrl(videoId),
                    originalUrl,
                    requiredText(snippet, "title", "유튜브 영상 제목을 찾을 수 없습니다."),
                    requiredText(snippet, "channelTitle", "유튜브 채널명을 찾을 수 없습니다."),
                    thumbnailUrl(snippet.path("thumbnails")),
                    parseDurationSeconds(requiredText(contentDetails, "duration", "유튜브 영상 길이 정보를 찾을 수 없습니다."))
            );
        } catch (SlateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "YouTube API 호출에 실패했습니다.");
        }
    }

    public String extractVideoId(String youtubeUrl) {
        String cleanUrl = cleanUrl(youtubeUrl);
        try {
            URI uri = URI.create(cleanUrl);
            String host = normalizeHost(uri.getHost());
            String path = uri.getPath() == null ? "" : uri.getPath();
            if ("youtu.be".equals(host)) {
                return requireVideoId(firstPathSegment(path));
            }
            if (!"youtube.com".equals(host)) {
                throw invalidUrl();
            }
            if ("/watch".equals(path)) {
                return requireVideoId(queryParam(uri.getRawQuery(), "v"));
            }
            if (path.startsWith("/embed/")) {
                return requireVideoId(pathSegmentAfter(path, "embed"));
            }
            if (path.startsWith("/shorts/")) {
                return requireVideoId(pathSegmentAfter(path, "shorts"));
            }
            throw invalidUrl();
        } catch (IllegalArgumentException ex) {
            throw invalidUrl();
        }
    }

    int parseDurationSeconds(String value) {
        try {
            return Math.toIntExact(Duration.parse(value).toSeconds());
        } catch (Exception ex) {
            throw new SlateException("유튜브 영상 길이 정보를 해석하지 못했습니다.");
        }
    }

    private JsonNode firstVideoItem(String body) {
        try {
            JsonNode items = objectMapper.readTree(body).path("items");
            if (!items.isArray() || items.isEmpty()) {
                throw new SlateException(HttpStatus.NOT_FOUND, "유튜브 영상을 찾을 수 없습니다.");
            }
            return items.get(0);
        } catch (SlateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "YouTube API 응답을 해석하지 못했습니다.");
        }
    }

    private String thumbnailUrl(JsonNode thumbnails) {
        for (String key : THUMBNAIL_PRIORITY) {
            String url = text(thumbnails.path(key), "url");
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private String requiredText(JsonNode node, String field, String message) {
        String value = text(node, field);
        if (value == null) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, message);
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String embedUrl(String videoId) {
        return EMBED_BASE_URL + videoId;
    }

    private String cleanUrl(String youtubeUrl) {
        if (!StringUtils.hasText(youtubeUrl)) {
            throw invalidUrl();
        }
        return youtubeUrl.trim();
    }

    private String normalizeHost(String host) {
        String normalized = Objects.toString(host, "").toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    private String firstPathSegment(String path) {
        return Arrays.stream(path.split("/"))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String pathSegmentAfter(String path, String marker) {
        List<String> segments = Arrays.stream(path.split("/"))
                .filter(StringUtils::hasText)
                .toList();
        int markerIndex = segments.indexOf(marker);
        return markerIndex >= 0 && markerIndex + 1 < segments.size() ? segments.get(markerIndex + 1) : null;
    }

    private String queryParam(String rawQuery, String key) {
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private String requireVideoId(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidUrl();
        }
        String videoId = value.trim();
        if (videoId.contains("/") || videoId.contains("?") || videoId.contains("&") || videoId.contains("=")) {
            throw invalidUrl();
        }
        return videoId;
    }

    private SlateException invalidUrl() {
        return new SlateException("유튜브 URL 형식이 올바르지 않습니다.");
    }
}
