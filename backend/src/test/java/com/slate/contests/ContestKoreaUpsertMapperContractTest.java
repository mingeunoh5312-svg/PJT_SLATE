package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ContestKoreaUpsertMapperContractTest {

    @Test
    void mapperInterfaceExposesCrawlerUpsertMethods() {
        assertThat(Arrays.stream(ContestMapper.class.getDeclaredMethods()).map(method -> method.getName()))
                .contains("selectContestBySource", "upsertContestFromCrawler");
    }

    @Test
    void upsertSqlContainsSourcePosterAndRepresentativeImagePathColumns() throws Exception {
        String xml = mapperXml();
        String upsert = between(xml, "<insert id=\"upsertContestFromCrawler\"", "</insert>");

        assertThat(upsert)
                .contains("source_name, source_external_id")
                .contains("#{sourceName,jdbcType=VARCHAR}")
                .contains("#{sourceExternalId,jdbcType=VARCHAR}")
                .contains("representative_image_path")
                .contains("poster_source_type")
                .contains("poster_original_url")
                .contains("poster_collected_at")
                .contains("source_permission_text")
                .contains("source_attribution")
                .contains("ON DUPLICATE KEY UPDATE")
                .contains("contest_id = LAST_INSERT_ID(contest_id)")
                .contains("source_url = VALUES(source_url)")
                .contains("source_category_code = VALUES(source_category_code)")
                .contains("source_collected_at = VALUES(source_collected_at)")
                .contains("source_updated_at = VALUES(source_updated_at)");
    }

    @Test
    void upsertUpdateClauseDoesNotOverwriteOwnershipCountersOrCreatedAt() throws Exception {
        String upsert = between(mapperXml(), "<insert id=\"upsertContestFromCrawler\"", "</insert>");
        String updateClause = upsert.substring(upsert.indexOf("ON DUPLICATE KEY UPDATE"));

        assertThat(updateClause)
                .doesNotContain("save_count =")
                .doesNotContain("created_at =")
                .doesNotContain("created_by =")
                .doesNotContain("requester_company_user_id =")
                .doesNotContain("source_request_id =")
                .contains("updated_at = NOW()");
    }

    @Test
    void generalUpdateContestStillDoesNotWriteCrawlSourceOrPosterColumns() throws Exception {
        String update = between(mapperXml(), "<update id=\"updateContest\"", "</update>");

        assertThat(update)
                .doesNotContain("source_name", "source_external_id", "source_url", "source_category_code",
                        "source_collected_at", "source_updated_at", "source_permission_text", "source_attribution",
                        "poster_source_type", "poster_original_url", "poster_collected_at",
                        "representative_image_path");
    }

    @Test
    void selectBySourceUsesSourceNameAndExternalIdOnly() throws Exception {
        String select = between(mapperXml(), "<select id=\"selectContestBySource\"", "</select>");

        assertThat(select)
                .contains("WHERE source_name = #{sourceName}")
                .contains("AND source_external_id = #{sourceExternalId}")
                .contains("representative_image_path AS representativeImagePath");
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
