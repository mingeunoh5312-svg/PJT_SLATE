package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class ContestKoreaPosterStorageServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final String POSTER_URL = "https://www.contestkorea.com/upload/contest/poster-2026.jpg";
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    @TempDir
    Path tempDir;

    @Test
    void disabledPosterDownloadSkipsWithoutHttpRequest() {
        Fixture fixture = fixture(properties(false));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest());

        assertThat(result.stored()).isFalse();
        assertThat(result.representativeImagePath()).isNull();
        fixture.server.verify();
    }

    @Test
    void nonAllowedPosterSourceTypeSkipsWithoutHttpRequest() {
        Fixture fixture = fixture(properties(true));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.posterSourceType = "NONE"));

        assertThat(result.stored()).isFalse();
        fixture.server.verify();
    }

    @Test
    void missingPermissionTextSkipsWithoutHttpRequest() {
        Fixture fixture = fixture(properties(true));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.sourcePermissionText = null));

        assertThat(result.stored()).isFalse();
        fixture.server.verify();
    }

    @Test
    void permissionTextWithoutRequiredPhraseSkipsWithoutHttpRequest() {
        Fixture fixture = fixture(properties(true));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.sourcePermissionText = "이미지 사용 가능"));

        assertThat(result.stored()).isFalse();
        fixture.server.verify();
    }

    @Test
    void missingPosterOriginalUrlSkipsWithoutHttpRequest() {
        Fixture fixture = fixture(properties(true));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.posterOriginalUrl = null));

        assertThat(result.stored()).isFalse();
        fixture.server.verify();
    }

    @Test
    void externalHostUrlFailsBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest(builder -> builder.posterOriginalUrl = "https://cdn.example.com/poster.jpg")))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("host");
        fixture.server.verify();
    }

    @Test
    void unsafeSchemeUrlsFailBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true));

        for (String url : List.of(
                "javascript:alert(1)",
                "data:image/png;base64,AAAA",
                "file:///tmp/poster.jpg"
        )) {
            assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest(builder -> builder.posterOriginalUrl = url)))
                    .isInstanceOf(SlateException.class);
        }
        fixture.server.verify();
    }

    @Test
    void userInfoUrlFailsBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest(
                builder -> builder.posterOriginalUrl = "https://user@www.contestkorea.com/upload/contest/poster.jpg"
        )))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("URL");
        fixture.server.verify();
    }

    @Test
    void redirectResponseFailsWithoutFollowingLocation() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, "https://cdn.example.com/poster.jpg"));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("응답 상태");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void jpegResponseWithJpegSignatureIsStored() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andExpect(header(HttpHeaders.USER_AGENT, "SlateBot/1.0 (contact: test@slate.test)"))
                .andRespond(withSuccess(jpeg(), MediaType.IMAGE_JPEG));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest());

        assertStored(result, "jpg");
        fixture.server.verify();
    }

    @Test
    void pngResponseWithPngSignatureIsStored() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess(png(), MediaType.IMAGE_PNG));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.posterOriginalUrl = POSTER_URL));

        assertStored(result, "png");
        fixture.server.verify();
    }

    @Test
    void jpegSignatureWithIncorrectPngContentTypeIsStoredAsJpeg() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess(jpeg(), MediaType.IMAGE_PNG));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest());

        assertStored(result, "jpg");
        fixture.server.verify();
    }

    @Test
    void webpResponseWithWebpSignatureIsStored() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess(webp(), MediaType.parseMediaType("image/webp")));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest(builder -> builder.posterOriginalUrl = POSTER_URL));

        assertStored(result, "webp");
        fixture.server.verify();
    }

    @Test
    void nonImageContentTypeFailsAndLeavesNoFile() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess("not image", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("JPEG");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void signatureMismatchFailsAndLeavesNoFile() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess("not-a-png".getBytes(), MediaType.IMAGE_PNG));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("JPEG");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void contentLengthAboveLimitFailsAndLeavesNoFile() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess(jpeg(), MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(MAX_BYTES + 1)));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("5MB");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void bodyAboveLimitFailsAndLeavesNoFile() throws Exception {
        Fixture fixture = fixture(properties(true));
        byte[] body = new byte[(int) MAX_BYTES + 1];
        body[0] = (byte) 0xff;
        body[1] = (byte) 0xd8;
        body[2] = (byte) 0xff;
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(responseWithoutContentLength(body, MediaType.IMAGE_JPEG));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("5MB");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void httpErrorResponseFailsAndLeavesNoFile() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("not found"));

        assertThatThrownBy(() -> fixture.service.storePosterIfAllowed(contest()))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("응답 상태");

        assertThat(regularFiles()).isEmpty();
        fixture.server.verify();
    }

    @Test
    void successPathIsRelativeToUploadRootAndUsesContestDateFolders() throws Exception {
        Fixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo(POSTER_URL))
                .andRespond(withSuccess(jpeg(), MediaType.IMAGE_JPEG));

        ContestKoreaPosterStorageResult result = fixture.service.storePosterIfAllowed(contest());

        assertThat(result.stored()).isTrue();
        assertThat(result.representativeImagePath())
                .startsWith("images/contest/2026/06/")
                .endsWith(".jpg");
        assertThat(Path.of(result.representativeImagePath()).isAbsolute()).isFalse();
        assertThat(tempDir.resolve(result.representativeImagePath()).normalize()).exists();
        assertThat(result.posterCollectedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 11, 0));
        fixture.server.verify();
    }

    private void assertStored(ContestKoreaPosterStorageResult result, String extension) throws Exception {
        assertThat(result.stored()).isTrue();
        assertThat(result.representativeImagePath()).matches("images/contest/2026/06/[0-9a-f\\-]{36}\\." + extension);
        assertThat(result.posterOriginalUrl()).isEqualTo(POSTER_URL);
        assertThat(result.posterSourceType()).isEqualTo("CONTESTKOREA_ALLOWED");
        assertThat(result.sourcePermissionText()).contains("콘테스트코리아 출처 표기");
        assertThat(tempDir.resolve(result.representativeImagePath())).exists();
    }

    private List<Path> regularFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(tempDir)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    private ResponseCreator responseWithoutContentLength(byte[] body, MediaType contentType) {
        return request -> {
            MockClientHttpResponse response = new MockClientHttpResponse(body, HttpStatus.OK);
            response.getHeaders().setContentType(contentType);
            return response;
        };
    }

    private Fixture fixture(ContestKoreaProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(
                new ContestKoreaPosterStorageService(properties, builder.build(), tempDir.toString(), FIXED_CLOCK),
                server
        );
    }

    private ContestKoreaProperties properties(boolean posterDownloadEnabled) {
        return new ContestKoreaProperties(
                true,
                "https://www.contestkorea.com",
                "/sub/list.php",
                "031210001",
                1,
                "SlateBot/1.0 (contact: test@slate.test)",
                1500L,
                5000L,
                10000L,
                1,
                30,
                posterDownloadEnabled,
                "콘테스트코리아 출처 표기",
                "CONTESTKOREA",
                "출처: 콘테스트코리아"
        );
    }

    private ContestKoreaNormalizedContest contest() {
        return contest(builder -> { });
    }

    private ContestKoreaNormalizedContest contest(Consumer<ContestBuilder> customizer) {
        ContestBuilder builder = new ContestBuilder();
        customizer.accept(builder);
        return builder.build();
    }

    private byte[] jpeg() {
        return new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    private byte[] png() {
        return new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
    }

    private byte[] webp() {
        return new byte[]{82, 73, 70, 70, 0, 0, 0, 0, 87, 69, 66, 80};
    }

    private static final class ContestBuilder {
        String posterSourceType = "CONTESTKOREA_ALLOWED";
        String posterOriginalUrl = POSTER_URL;
        String sourcePermissionText = "콘테스트코리아 출처 표기 후 포스터 사용 가능";

        ContestKoreaNormalizedContest build() {
            return new ContestKoreaNormalizedContest(
                    "EXTERNAL",
                    "2026 Slate 단편영화 공모전",
                    "단편영화와 영상 콘텐츠를 모집합니다.",
                    "사진•영상•영화제",
                    "총상금 1,000만원 / 대상 500만원",
                    10_000_000L,
                    5_000_000L,
                    "한국영상협회",
                    "SCHOOL_ASSOCIATION",
                    null,
                    posterSourceType,
                    posterOriginalUrl,
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    "https://apply.example.com/slate-film",
                    "대학생 및 일반인",
                    List.of("UNIVERSITY", "ADULT"),
                    List.of("SEOUL"),
                    "연출, 촬영",
                    "영화",
                    LocalDateTime.of(2026, 6, 10, 0, 0),
                    LocalDateTime.of(2026, 7, 8, 23, 59, 59),
                    "OPEN",
                    "CONTESTKOREA",
                    "202606170004",
                    "https://www.contestkorea.com/sub/view.php?str_no=202606170004",
                    "031210001",
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    LocalDateTime.of(2026, 6, 23, 11, 0),
                    sourcePermissionText,
                    "출처: 콘테스트코리아"
            );
        }
    }

    private record Fixture(ContestKoreaPosterStorageService service, MockRestServiceServer server) { }
}
