package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class ContestKoreaPropertiesTest {

    @Test
    void defaultsAreSafeForManualFutureCrawlerUse() {
        ContestKoreaProperties properties = defaults();

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.baseUrl()).isEqualTo("https://www.contestkorea.com");
        assertThat(properties.listPath()).isEqualTo("/sub/list.php");
        assertThat(properties.categoryCode()).isEqualTo("031210001");
        assertThat(properties.intGbn()).isEqualTo(1);
        assertThat(properties.requestDelayMillis()).isGreaterThanOrEqualTo(1000L);
        assertThat(properties.maxPages()).isEqualTo(10);
        assertThat(properties.maxItemsPerRun()).isEqualTo(100);
        assertThat(properties.posterDownloadEnabled()).isTrue();
        assertThat(properties.requiredPermissionText()).contains("콘테스트코리아 출처 표기");
        assertThat(properties.sourceName()).isEqualTo("CONTESTKOREA");
        assertThat(properties.sourceAttribution()).isEqualTo("출처: 콘테스트코리아");
    }

    @Test
    void bindsConfiguredValuesAndBuildsListRequestParts() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("slate.public-data.contest-korea.enabled", "true")
                .withProperty("slate.public-data.contest-korea.base-url", "https://www.contestkorea.com/")
                .withProperty("slate.public-data.contest-korea.list-path", "/sub/list.php")
                .withProperty("slate.public-data.contest-korea.category-code", "031210001")
                .withProperty("slate.public-data.contest-korea.int-gbn", "1")
                .withProperty("slate.public-data.contest-korea.user-agent", "SlateBot/1.0 (contact: ops@slate.test)")
                .withProperty("slate.public-data.contest-korea.request-delay-millis", "2000")
                .withProperty("slate.public-data.contest-korea.connect-timeout-millis", "3000")
                .withProperty("slate.public-data.contest-korea.read-timeout-millis", "7000")
                .withProperty("slate.public-data.contest-korea.max-pages", "2")
                .withProperty("slate.public-data.contest-korea.max-items-per-run", "40")
                .withProperty("slate.public-data.contest-korea.poster-download-enabled", "false")
                .withProperty("slate.public-data.contest-korea.required-permission-text", "콘테스트코리아 출처 표기")
                .withProperty("slate.public-data.contest-korea.source-name", "CONTESTKOREA")
                .withProperty("slate.public-data.contest-korea.source-attribution", "출처: 콘테스트코리아");

        ContestKoreaProperties properties = Binder.get(environment)
                .bind("slate.public-data.contest-korea", ContestKoreaProperties.class)
                .get();

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.baseUrl()).isEqualTo("https://www.contestkorea.com");
        assertThat(properties.listUri().toString()).isEqualTo("https://www.contestkorea.com/sub/list.php");
        assertThat(properties.listQueryParams())
                .containsEntry("Txt_bcode", "031210001")
                .containsEntry("int_gbn", "1");
        assertThat(properties.posterDownloadEnabled()).isFalse();
    }

    @Test
    void rejectsInvalidUrlAndListPath() {
        assertThatThrownBy(() -> properties("ftp://www.contestkorea.com", "/sub/list.php", 1500L, 1, 30, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");
        assertThatThrownBy(() -> properties("https://", "/sub/list.php", 1500L, 1, 30, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties("https://www.contestkorea.com", "sub/list.php", 1500L, 1, 30, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("listPath");
    }

    @Test
    void rejectsBlankUserAgentAndUnsafeLimits() {
        assertThatThrownBy(() -> properties("https://www.contestkorea.com", "/sub/list.php", 1500L, 1, 30, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userAgent");
        assertThatThrownBy(() -> properties("https://www.contestkorea.com", "/sub/list.php", 999L, 1, 30, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestDelayMillis");
        assertThatThrownBy(() -> properties("https://www.contestkorea.com", "/sub/list.php", 1500L, 11, 30, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPages");
        assertThatThrownBy(() -> properties("https://www.contestkorea.com", "/sub/list.php", 1500L, 1, 101, "SlateBot/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxItemsPerRun");
    }

    @Test
    void rejectsBlankPermissionAndSourceMetadataDefaultsRemainExplicit() {
        assertThatThrownBy(() -> new ContestKoreaProperties(
                false,
                "https://www.contestkorea.com",
                "/sub/list.php",
                "031210001",
                1,
                "SlateBot/1.0",
                1500L,
                5000L,
                10000L,
                1,
                30,
                true,
                " ",
                "CONTESTKOREA",
                "출처: 콘테스트코리아"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requiredPermissionText");

        ContestKoreaProperties properties = defaults();
        assertThat(properties.requiredPermissionText()).contains("콘테스트코리아 출처 표기");
        assertThat(properties.sourceName()).isEqualTo("CONTESTKOREA");
        assertThat(properties.sourceAttribution()).isEqualTo("출처: 콘테스트코리아");
    }

    private ContestKoreaProperties defaults() {
        return new ContestKoreaProperties(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private ContestKoreaProperties properties(
            String baseUrl,
            String listPath,
            Long requestDelayMillis,
            Integer maxPages,
            Integer maxItemsPerRun,
            String userAgent
    ) {
        return new ContestKoreaProperties(
                false,
                baseUrl,
                listPath,
                "031210001",
                1,
                userAgent,
                requestDelayMillis,
                5000L,
                10000L,
                maxPages,
                maxItemsPerRun,
                true,
                "콘테스트코리아 출처 표기",
                "CONTESTKOREA",
                "출처: 콘테스트코리아"
        );
    }
}
