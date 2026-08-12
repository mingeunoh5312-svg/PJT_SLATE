package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ContestDataContractTest {

    @Test
    void publicAndUrgentQueriesAreDeadlineBasedAndDoNotJoinFitCache() throws Exception {
        String xml = mapperXml();
        String publicList = between(xml, "<select id=\"selectContests\"", "</select>");
        String urgent = between(xml, "<select id=\"selectUrgentContests\"", "</select>");
        String detail = between(xml, "<select id=\"selectContestById\"", "</select>");

        assertThat(publicList)
                .contains("c.deadline_at ASC")
                .contains("JSON_CONTAINS(c.target_codes_json")
                .contains("JSON_CONTAINS(c.region_codes_json")
                .contains("c.organizer_type IN")
                .contains("c.total_prize_amount")
                .contains("c.first_prize_amount")
                .doesNotContain("benefit_codes_json", "manager_tip_yn", "award_info_yn")
                .doesNotContain("contest_fit_cache", "fit_score", "fitScore");
        assertThat(detail).doesNotContain("contest_fit_cache", "fit_score", "fitScore");
        assertThat(urgent)
                .contains("c.status = 'OPEN'")
                .contains("c.deadline_at &gt; NOW()")
                .contains("ORDER BY c.deadline_at ASC, c.contest_id ASC")
                .contains("LIMIT #{limit}")
                .doesNotContain("contest_fit_cache", "fit_score", "fitScore");
    }

    @Test
    void uploadedImagesHaveDedicatedContestAndRequestUrls() throws Exception {
        assertThat(mapperXml())
                .contains("representativeImagePath")
                .contains("/api/media/images/contest/")
                .contains("/api/media/images/contest_request/")
                .contains("clearContestOpenRequestImagePath");
    }

    private String mapperXml() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/mappers/ContestMapper.xml")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertThat(from).isGreaterThanOrEqualTo(0);
        assertThat(to).isGreaterThan(from);
        return source.substring(from, to + end.length());
    }
}
