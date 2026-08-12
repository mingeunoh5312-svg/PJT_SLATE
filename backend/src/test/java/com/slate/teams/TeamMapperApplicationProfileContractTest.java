package com.slate.teams;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TeamMapperApplicationProfileContractTest {

    @Test
    void applicationListIncludesNullableActiveApplicantProfileId() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mappers/TeamMapper.xml")) {
            assertThat(input).isNotNull();
            String mapper = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String applicationQuery = mapper.substring(
                    mapper.indexOf("<select id=\"selectApplicationsByTeamId\""),
                    mapper.indexOf("</select>", mapper.indexOf("<select id=\"selectApplicationsByTeamId\""))
            );

            assertThat(applicationQuery)
                    .contains("ta.applicant_user_id AS applicantUserId")
                    .contains("mp.profile_id AS applicantProfileId")
                    .contains("LEFT JOIN member_profile mp")
                    .contains("mp.user_id = ta.applicant_user_id")
                    .contains("mp.status = 'ACTIVE'");
        }
    }
}
