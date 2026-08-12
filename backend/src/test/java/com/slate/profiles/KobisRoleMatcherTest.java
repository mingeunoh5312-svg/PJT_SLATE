package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class KobisRoleMatcherTest {

    private final KobisRoleMatcher matcher = new KobisRoleMatcher();

    @Test
    void verifiesWhenPeopleNameExactlyMatchesAndRoleIsInSameGroup() {
        KobisMovieDetail detail = new KobisMovieDetail(
                "20124079",
                "광해, 왕이 된 남자",
                "Masquerade",
                "2012",
                "20120913",
                "사극,드라마",
                List.of(),
                List.of(new KobisCredit("이병헌", "LEE Byung-hun", "배우", "ACTOR")),
                List.of(),
                "{}"
        );

        KobisVerificationMatch result = matcher.match(detail, "이병헌", "출연");

        assertThat(result.verificationStatus()).isEqualTo("VERIFIED");
        assertThat(result.providerPersonName()).isEqualTo("이병헌");
        assertThat(result.providerRoleName()).isEqualTo("배우");
        assertThat(result.matchedRoleGroup()).isEqualTo("배우/출연");
    }

    @Test
    void doesNotVerifyWhenPeopleNameOnlyPartiallyMatches() {
        KobisMovieDetail detail = new KobisMovieDetail(
                "20124079",
                "광해, 왕이 된 남자",
                "Masquerade",
                "2012",
                "20120913",
                "사극,드라마",
                List.of(new KobisCredit("추창민", "CHOO Chang-min", "감독", "DIRECTOR")),
                List.of(),
                List.of(),
                "{}"
        );

        KobisVerificationMatch result = matcher.match(detail, "추창", "감독");

        assertThat(result.verificationStatus()).isEqualTo("NOT_VERIFIED");
    }

    @Test
    void treatsNullCreditListsAsEmptyLists() {
        KobisMovieDetail detail = new KobisMovieDetail(
                "20124079",
                "광해, 왕이 된 남자",
                "Masquerade",
                "2012",
                "20120913",
                "사극,드라마",
                null,
                null,
                null,
                "{}"
        );

        KobisVerificationMatch result = matcher.match(detail, "이병헌", "배우");

        assertThat(result.verificationStatus()).isEqualTo("NOT_VERIFIED");
    }
}
