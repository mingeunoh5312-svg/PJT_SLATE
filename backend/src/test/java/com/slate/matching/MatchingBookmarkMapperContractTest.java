package com.slate.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MatchingBookmarkMapperContractTest {

    @Test
    void teamBookmarkReadAndDeleteAreScopedToCurrentUser() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mappers/MatchingMapper.xml")) {
            assertThat(input).isNotNull();
            String mapper = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String readQuery = statement(mapper, "select", "selectTeamBookmarks");
            String deleteQuery = statement(mapper, "delete", "deleteBookmark");

            assertThat(readQuery)
                    .contains("mb.user_id = #{userId}")
                    .contains("mb.target_type = 'TEAM'");
            assertThat(deleteQuery)
                    .contains("user_id = #{userId}")
                    .contains("target_type = #{targetType}")
                    .contains("target_id = #{targetId}");
        }
    }

    private String statement(String mapper, String element, String id) {
        int start = mapper.indexOf("<" + element + " id=\"" + id + "\"");
        return mapper.substring(start, mapper.indexOf("</" + element + ">", start));
    }
}
