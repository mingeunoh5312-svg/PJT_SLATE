package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BoardFullIntegrationSqlContractTest {

    @Test
    void mapperUsesLikeOnlyRankingFollowerRankingAndControlledFilters() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mappers/BoardMapper.xml"));

        assertThat(xml).contains("COALESCE(p.free_category, 'FREE') = #{freeCategory}");
        assertThat(xml).contains("COALESCE(wi.work_type, 'OTHER') = #{workType}");
        assertThat(xml).contains("ORDER BY p.like_count DESC, p.created_at DESC, p.post_id DESC");
        assertThat(xml).contains("COUNT(follower_profile.profile_id) AS followerCount");
        assertThat(xml).contains("current_follow.follower_user_id = #{currentUserId}");
        assertThat(xml).doesNotContain("COUNT(ti.invitation_id) AS invitationCount");
    }

    @Test
    void migrationIsIdempotentAndBackfillsLegacyRows() throws Exception {
        String sql = Files.readString(Path.of("../sql/10_board_full_integration_schema.sql"));

        assertThat(sql).contains("information_schema.columns");
        assertThat(sql).contains("information_schema.statistics");
        assertThat(sql).contains("SET free_category = 'FREE'");
        assertThat(sql).contains("SET work_type = 'OTHER'");
        assertThat(sql).contains("bl.active_yn = 'Y'");
    }

    @Test
    void genreRelationAndPeriodRankingContractsAreExplicit() throws Exception {
        String schema = Files.readString(Path.of("../sql/01_schema.sql"));
        String migration = Files.readString(Path.of("../sql/11_board_search_genre_period_schema.sql"));
        String xml = Files.readString(Path.of("src/main/resources/mappers/BoardMapper.xml"));

        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS work_genre");
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS team_work_approval_genre");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS work_genre");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS team_work_approval_genre");
        assertThat(xml).contains("wgf.genre_id = #{genreId}");
        assertThat(xml).contains("period == 'WEEKLY_WORK'");
        assertThat(xml).contains("INTERVAL 7 DAY");
        assertThat(xml).contains("period == 'MONTHLY_WORK'");
        assertThat(xml).contains("INTERVAL 30 DAY");
        assertThat(xml).contains("ORDER BY p.like_count DESC, p.created_at DESC, p.post_id DESC");
        assertThat(xml).doesNotContain("selectMonthlyWorkRanking");
    }
}
