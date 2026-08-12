package com.slate.contests;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

import com.slate.common.SlateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ContestKoreaClient {

    private static final String DETAIL_PATH = "/sub/view.php";

    private final ContestKoreaProperties properties;
    private final RestClient restClient;
    private final ContestKoreaDelayStrategy delayStrategy;
    private boolean requestSent;

    @Autowired
    public ContestKoreaClient(ContestKoreaProperties properties, RestClient.Builder restClientBuilder) {
        this(properties, buildRestClient(properties, restClientBuilder), threadSleepDelay());
    }

    ContestKoreaClient(
            ContestKoreaProperties properties,
            RestClient restClient,
            ContestKoreaDelayStrategy delayStrategy
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.delayStrategy = delayStrategy;
    }

    public String fetchListPage(int page) {
        requireEnabled();
        if (page < 1) {
            throw new SlateException("콘테스트코리아 목록 page는 1 이상이어야 합니다.");
        }
        if (page > properties.maxPages()) {
            throw new SlateException("콘테스트코리아 목록 page가 maxPages 설정을 초과했습니다.");
        }
        URI uri = UriComponentsBuilder.fromUri(properties.listUri())
                .queryParam("int_gbn", properties.intGbn())
                .queryParam("Txt_bcode", properties.categoryCode())
                .queryParam("page", page)
                .encode()
                .build()
                .toUri();
        return fetchHtml(uri);
    }

    public String fetchDetailPage(String detailHrefOrUrl) {
        requireEnabled();
        URI uri = detailUri(detailHrefOrUrl);
        return fetchHtml(uri);
    }

    URI detailUri(String detailHrefOrUrl) {
        if (!StringUtils.hasText(detailHrefOrUrl)) {
            throw invalidDetailUrl();
        }
        String value = detailHrefOrUrl.trim();
        if (value.startsWith("//")) {
            throw invalidDetailUrl();
        }

        URI raw;
        try {
            raw = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw invalidDetailUrl();
        }

        URI uri;
        if (StringUtils.hasText(raw.getScheme())) {
            uri = raw;
        } else {
            String relative = normalizeDetailRelativePath(value);
            try {
                uri = properties.baseUri().resolve(URI.create(relative));
            } catch (IllegalArgumentException ex) {
                throw invalidDetailUrl();
            }
        }

        validateDetailUri(uri);
        return UriComponentsBuilder.fromUri(uri).fragment(null).build(true).toUri();
    }

    private String fetchHtml(URI uri) {
        delayBeforeConsecutiveRequest();
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.userAgent())
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 HTML 응답 상태가 성공이 아닙니다.");
            }
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
                throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 HTML 응답 Content-Type이 올바르지 않습니다.");
            }
            String body = response.getBody();
            if (!StringUtils.hasText(body)) {
                throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 HTML 응답 본문이 비어 있습니다.");
            }
            return body;
        } catch (SlateException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 HTML 요청이 실패했습니다.");
        } catch (RestClientException ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 HTML 요청 중 오류가 발생했습니다.");
        } finally {
            requestSent = true;
        }
    }

    private void requireEnabled() {
        if (!Boolean.TRUE.equals(properties.enabled())) {
            throw new SlateException("콘테스트코리아 크롤러가 비활성화되어 있습니다.");
        }
    }

    private void delayBeforeConsecutiveRequest() {
        if (requestSent) {
            delayStrategy.delay(properties.requestDelayMillis());
        }
    }

    private String normalizeDetailRelativePath(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        if (value.startsWith("sub/view.php")) {
            return "/" + value;
        }
        if (value.startsWith("view.php")) {
            return "/sub/" + value;
        }
        throw invalidDetailUrl();
    }

    private void validateDetailUri(URI uri) {
        String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw invalidDetailUrl();
        }
        if (StringUtils.hasText(uri.getUserInfo())) {
            throw invalidDetailUrl();
        }
        if (!normalizeHost(properties.baseUri().getHost()).equals(normalizeHost(uri.getHost()))) {
            throw invalidDetailUrl();
        }
        if (!DETAIL_PATH.equals(Objects.toString(uri.getPath(), ""))) {
            throw invalidDetailUrl();
        }
    }

    private static RestClient buildRestClient(ContestKoreaProperties properties, RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis()));
        return restClientBuilder.requestFactory(requestFactory).build();
    }

    private static ContestKoreaDelayStrategy threadSleepDelay() {
        return millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new SlateException(HttpStatus.SERVICE_UNAVAILABLE, "콘테스트코리아 요청 대기 중 인터럽트가 발생했습니다.");
            }
        };
    }

    private String normalizeHost(String host) {
        return Objects.toString(host, "").toLowerCase(Locale.ROOT);
    }

    private SlateException invalidDetailUrl() {
        return new SlateException("콘테스트코리아 상세 URL 형식이 올바르지 않습니다.");
    }
}
