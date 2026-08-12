package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProfileMapperPortfolioVerificationContractTest {

    @Test
    void portfolioQueriesMapVerifiedAsBoolean() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mappers/ProfileMapper.xml")) {
            assertThat(input).isNotNull();
            String mapper = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(mapper)
                    .contains("<resultMap id=\"portfolioItemResultMap\" type=\"map\" autoMapping=\"true\">")
                    .contains("property=\"verified\" column=\"verified\" javaType=\"java.lang.Boolean\"");

            for (String id : new String[]{"selectPortfolioItems", "selectPortfolioItemById", "selectOwnedPortfolioItem"}) {
                assertThat(statement(mapper, id))
                        .contains("resultMap=\"portfolioItemResultMap\"")
                        .contains("pi.credit_name AS creditName")
                        .contains("pv.provider_movie_code AS providerMovieCode")
                        .contains("pv.provider_person_name AS providerPersonName")
                        .contains("pv.provider_role_name AS providerRoleName")
                        .contains("pv.matched_role_group AS matchedRoleGroup")
                        .contains("pv.verification_status AS verificationStatus");
            }

            assertThat(statement(mapper, "insertPortfolioItem"))
                    .contains("credit_name")
                    .contains("#{creditName}");
            assertThat(statement(mapper, "updatePortfolioItem"))
                    .contains("credit_name = #{creditName}");
        }
    }

    private String statement(String mapper, String id) {
        int start = mapper.indexOf("id=\"" + id + "\"");
        int tagStart = mapper.lastIndexOf('<', start);
        int nameEnd = mapper.indexOf(' ', tagStart);
        String tagName = mapper.substring(tagStart + 1, nameEnd);
        return mapper.substring(tagStart, mapper.indexOf("</" + tagName + ">", start));
    }
}
