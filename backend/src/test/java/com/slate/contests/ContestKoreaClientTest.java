package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ContestKoreaClientTest {

    @Test
    void disabledClientFailsBeforeHttpRequest() {
        Fixture fixture = fixture(properties(false, 1, 30, 1500L));

        assertThatThrownBy(() -> fixture.client.fetchListPage(1))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("비활성화");
        fixture.server.verify();
        assertThat(fixture.delay.delays).isEmpty();
    }

    @Test
    void fetchListPageBuildsConfiguredUrlAndUserAgent() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=1"))
                .andExpect(header(HttpHeaders.USER_AGENT, "SlateBot/1.0 (contact: test@slate.test)"))
                .andRespond(withSuccess("<html>list</html>", MediaType.TEXT_HTML));

        assertThat(fixture.client.fetchListPage(1)).contains("list");

        fixture.server.verify();
        assertThat(fixture.delay.delays).isEmpty();
    }

    @Test
    void invalidListPageFailsBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));

        assertThatThrownBy(() -> fixture.client.fetchListPage(0))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("1 이상");
        fixture.server.verify();
    }

    @Test
    void listPageAboveMaxPagesFailsBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));

        assertThatThrownBy(() -> fixture.client.fetchListPage(2))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("maxPages");
        fixture.server.verify();
    }

    @Test
    void detailPageAllowsSameOriginAbsoluteUrl() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/view.php?str_no=12345"))
                .andRespond(withSuccess("<html>detail</html>", MediaType.TEXT_HTML));

        assertThat(fixture.client.fetchDetailPage("https://www.contestkorea.com/sub/view.php?str_no=12345"))
                .contains("detail");

        fixture.server.verify();
    }

    @Test
    void detailPageAllowsContestKoreaViewPath() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/view.php?str_no=12345"))
                .andRespond(withSuccess("<html>detail</html>", MediaType.TEXT_HTML));

        assertThat(fixture.client.fetchDetailPage("/sub/view.php?str_no=12345"))
                .contains("detail");

        fixture.server.verify();
    }

    @Test
    void detailPageAllowsRelativeViewPath() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/view.php?str_no=12345"))
                .andRespond(withSuccess("<html>detail</html>", MediaType.TEXT_HTML));

        assertThat(fixture.client.fetchDetailPage("view.php?str_no=12345"))
                .contains("detail");

        fixture.server.verify();
    }

    @Test
    void detailPageRejectsExternalHostBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));

        assertThatThrownBy(() -> fixture.client.fetchDetailPage("https://example.com/sub/view.php?str_no=12345"))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("상세 URL");
        fixture.server.verify();
    }

    @Test
    void detailPageRejectsUnsafeSchemesBeforeHttpRequest() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));

        for (String value : List.of(
                "javascript:alert(1)",
                "data:text/html;base64,PGh0bWw+",
                "file:///tmp/contest.html"
        )) {
            assertThatThrownBy(() -> fixture.client.fetchDetailPage(value))
                    .isInstanceOf(SlateException.class)
                    .hasMessageContaining("상세 URL");
        }
        fixture.server.verify();
    }

    @Test
    void httpErrorResponseFails() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>not found</html>"));

        assertThatThrownBy(() -> fixture.client.fetchListPage(1))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("요청이 실패");

        fixture.server.verify();
    }

    @Test
    void nonHtmlContentTypeFails() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=1"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client.fetchListPage(1))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("Content-Type");

        fixture.server.verify();
    }

    @Test
    void blankBodyFails() {
        Fixture fixture = fixture(properties(true, 1, 30, 1500L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=1"))
                .andRespond(withSuccess(" ", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> fixture.client.fetchListPage(1))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("본문");

        fixture.server.verify();
    }

    @Test
    void consecutiveRequestsUseConfiguredDelay() {
        Fixture fixture = fixture(properties(true, 2, 30, 1750L));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=1"))
                .andRespond(withSuccess("<html>list1</html>", MediaType.TEXT_HTML));
        fixture.server.expect(requestTo("https://www.contestkorea.com/sub/list.php?int_gbn=1&Txt_bcode=031210001&page=2"))
                .andRespond(withSuccess("<html>list2</html>", MediaType.TEXT_HTML));

        fixture.client.fetchListPage(1);
        fixture.client.fetchListPage(2);

        fixture.server.verify();
        assertThat(fixture.delay.delays).containsExactly(1750L);
    }

    private Fixture fixture(ContestKoreaProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FakeDelay delay = new FakeDelay();
        return new Fixture(new ContestKoreaClient(properties, builder.build(), delay), server, delay);
    }

    private ContestKoreaProperties properties(boolean enabled, int maxPages, int maxItemsPerRun, long requestDelayMillis) {
        return new ContestKoreaProperties(
                enabled,
                "https://www.contestkorea.com",
                "/sub/list.php",
                "031210001",
                1,
                "SlateBot/1.0 (contact: test@slate.test)",
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

    private record Fixture(ContestKoreaClient client, MockRestServiceServer server, FakeDelay delay) { }

    private static final class FakeDelay implements ContestKoreaDelayStrategy {
        private final List<Long> delays = new ArrayList<>();

        @Override
        public void delay(long millis) {
            delays.add(millis);
        }
    }
}
