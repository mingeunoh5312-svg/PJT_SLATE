package com.slate.contests;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.slate.common.SlateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ContestKoreaPosterStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final String POSTER_ALLOWED = "CONTESTKOREA_ALLOWED";
    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");

    private final ContestKoreaProperties properties;
    private final RestClient restClient;
    private final Path uploadRoot;
    private final Clock clock;

    @Autowired
    public ContestKoreaPosterStorageService(
            ContestKoreaProperties properties,
            RestClient.Builder restClientBuilder,
            @Value("${slate.upload.dir:${SLATE_UPLOAD_DIR:uploads}}") String uploadDir
    ) {
        this(properties, buildRestClient(properties, restClientBuilder), uploadDir, Clock.systemDefaultZone());
    }

    ContestKoreaPosterStorageService(
            ContestKoreaProperties properties,
            RestClient restClient,
            String uploadDir,
            Clock clock
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.clock = clock;
    }

    public ContestKoreaPosterStorageResult storePosterIfAllowed(ContestKoreaNormalizedContest contest) {
        if (contest == null) {
            throw new SlateException("콘테스트코리아 정규화 결과가 필요합니다.");
        }
        if (!shouldStore(contest)) {
            return ContestKoreaPosterStorageResult.skipped(contest);
        }
        URI posterUri = requireSafePosterUri(contest.posterOriginalUrl());
        DownloadedPoster poster = downloadPoster(posterUri);
        LocalDateTime storedAt = LocalDateTime.now(clock);
        String representativeImagePath = store(poster.bytes(), poster.extension());
        return ContestKoreaPosterStorageResult.stored(representativeImagePath, posterUri.toString(), storedAt, contest);
    }

    public void deleteStoredImmediately(String storedPath) {
        String normalized = textOrNull(storedPath);
        if (normalized == null) {
            return;
        }
        Path path = uploadRoot.resolve(normalized).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "콘테스트코리아 포스터 저장 경로가 올바르지 않습니다.");
        }
        deleteQuietly(path);
    }

    private boolean shouldStore(ContestKoreaNormalizedContest contest) {
        if (!Boolean.TRUE.equals(properties.posterDownloadEnabled())) {
            return false;
        }
        if (!POSTER_ALLOWED.equals(normalizeCode(contest.posterSourceType()))) {
            return false;
        }
        if (!StringUtils.hasText(contest.sourcePermissionText())) {
            return false;
        }
        if (!contest.sourcePermissionText().contains(properties.requiredPermissionText())) {
            return false;
        }
        return StringUtils.hasText(contest.posterOriginalUrl());
    }

    private URI requireSafePosterUri(String value) {
        URI uri;
        try {
            uri = URI.create(value.trim()).normalize();
        } catch (Exception ex) {
            throw new SlateException("콘테스트코리아 포스터 URL 형식이 올바르지 않습니다.");
        }
        String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
        if (!List.of("http", "https").contains(scheme)) {
            throw new SlateException("콘테스트코리아 포스터 URL은 HTTP 또는 HTTPS만 허용합니다.");
        }
        if (!StringUtils.hasText(uri.getHost()) || StringUtils.hasText(uri.getUserInfo())) {
            throw new SlateException("콘테스트코리아 포스터 URL 형식이 올바르지 않습니다.");
        }
        if (!normalizeHost(properties.baseUri().getHost()).equals(normalizeHost(uri.getHost()))) {
            throw new SlateException("콘테스트코리아 포스터 URL host가 허용 범위를 벗어났습니다.");
        }
        return uri;
    }

    private DownloadedPoster downloadPoster(URI uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, properties.userAgent())
                    .accept(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, IMAGE_WEBP)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 포스터 응답 상태가 성공이 아닙니다.");
                        }
                        long contentLength = response.getHeaders().getContentLength();
                        if (contentLength > MAX_BYTES) {
                            throw new SlateException("콘테스트코리아 포스터는 최대 5MB까지 저장할 수 있습니다.");
                        }
                        byte[] bytes = readLimited(response.getBody());
                        if (bytes.length == 0) {
                            throw new SlateException("콘테스트코리아 포스터 응답 본문이 비어 있습니다.");
                        }
                        String extension = signatureExtension(bytes);
                        if (extension == null) {
                            throw new SlateException("콘테스트코리아 포스터 파일 내용이 JPEG, PNG, WebP가 아닙니다.");
                        }
                        return new DownloadedPoster(bytes, extension);
                    });
        } catch (SlateException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new SlateException(HttpStatus.BAD_GATEWAY, "콘테스트코리아 포스터 요청 중 오류가 발생했습니다.");
        }
    }

    private byte[] readLimited(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_BYTES) {
                throw new SlateException("콘테스트코리아 포스터는 최대 5MB까지 저장할 수 있습니다.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String signatureExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 255) == 137 && h[1] == 80 && h[2] == 78 && h[3] == 71
                && h[4] == 13 && h[5] == 10 && h[6] == 26 && h[7] == 10) {
            return "png";
        }
        if (h.length >= 12 && h[0] == 82 && h[1] == 73 && h[2] == 70 && h[3] == 70
                && h[8] == 87 && h[9] == 69 && h[10] == 66 && h[11] == 80) {
            return "webp";
        }
        return null;
    }

    private String extension(MediaType contentType) {
        if (contentType == null) {
            throw new SlateException("콘테스트코리아 포스터 Content-Type이 비어 있습니다.");
        }
        String type = contentType.getType().toLowerCase(Locale.ROOT);
        String subtype = contentType.getSubtype().toLowerCase(Locale.ROOT);
        String value = type + "/" + subtype;
        return switch (value) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new SlateException("콘테스트코리아 포스터는 JPEG, PNG, WebP만 저장할 수 있습니다.");
        };
    }

    private void validateSignature(DownloadedPoster poster) {
        byte[] h = poster.bytes();
        boolean valid = switch (poster.extension()) {
            case "jpg" -> h.length >= 3 && (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255;
            case "png" -> h.length >= 8 && (h[0] & 255) == 137 && h[1] == 80 && h[2] == 78 && h[3] == 71
                    && h[4] == 13 && h[5] == 10 && h[6] == 26 && h[7] == 10;
            case "webp" -> h.length >= 12 && h[0] == 82 && h[1] == 73 && h[2] == 70 && h[3] == 70
                    && h[8] == 87 && h[9] == 69 && h[10] == 66 && h[11] == 80;
            default -> false;
        };
        if (!valid) {
            throw new SlateException("콘테스트코리아 포스터 파일 내용이 Content-Type과 일치하지 않습니다.");
        }
    }

    private String store(byte[] bytes, String extension) {
        LocalDate now = LocalDate.now(clock);
        Path dir = uploadRoot.resolve(Path.of("images", "contest", String.valueOf(now.getYear()), "%02d".formatted(now.getMonthValue()))).normalize();
        if (!dir.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "콘테스트코리아 포스터 저장 경로가 올바르지 않습니다.");
        }
        Path destination = null;
        try {
            Files.createDirectories(dir);
            destination = dir.resolve(UUID.randomUUID() + "." + extension).normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "콘테스트코리아 포스터 저장 경로가 올바르지 않습니다.");
            }
            Files.write(destination, bytes);
            return uploadRoot.relativize(destination).toString().replace('\\', '/');
        } catch (SlateException ex) {
            deleteQuietly(destination);
            throw ex;
        } catch (Exception ex) {
            deleteQuietly(destination);
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "콘테스트코리아 포스터 저장 중 오류가 발생했습니다.");
        }
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeHost(String host) {
        return Objects.toString(host, "").toLowerCase(Locale.ROOT);
    }

    private String textOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void deleteQuietly(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
        }
    }

    private static RestClient buildRestClient(ContestKoreaProperties properties, RestClient.Builder restClientBuilder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis()));
        return restClientBuilder.requestFactory(requestFactory).build();
    }

    private record DownloadedPoster(byte[] bytes, String extension) { }
}
