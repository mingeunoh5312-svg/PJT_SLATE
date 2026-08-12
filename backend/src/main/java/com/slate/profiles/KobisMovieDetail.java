package com.slate.profiles;

import java.util.List;

public record KobisMovieDetail(
        String movieCd,
        String movieNm,
        String movieNmEn,
        String prdtYear,
        String openDt,
        String genreAlt,
        List<KobisCredit> directors,
        List<KobisCredit> actors,
        List<KobisCredit> staffs,
        String rawResponseJson
) {
    public KobisMovieDetail {
        directors = directors == null ? List.of() : directors;
        actors = actors == null ? List.of() : actors;
        staffs = staffs == null ? List.of() : staffs;
    }
}
