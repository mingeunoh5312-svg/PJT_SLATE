package com.slate.profiles;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KobisClient {

    private static final Logger log = LoggerFactory.getLogger(KobisClient.class);

    private final KobisProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public KobisClient(KobisProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public boolean hasApiKey() {
        return properties.hasApiKey();
    }

    public List<Map<String, Object>> searchMovies(String keyword, Integer limit) {
        String cleanKeyword = clean(keyword);
        if (!properties.hasApiKey() || cleanKeyword == null || cleanKeyword.length() < 2) {
            if (!properties.hasApiKey()) {
                log.warn("KOBIS movie search skipped because API key is not configured.");
            }
            return List.of();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                    .path("/movie/searchMovieList.json")
                    .queryParam("key", properties.apiKey())
                    .queryParam("movieNm", cleanKeyword)
                    .encode()
                    .build()
                    .toUri();
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            JsonNode movieList = objectMapper.readTree(body).path("movieListResult").path("movieList");
            if (!movieList.isArray()) {
                log.warn("KOBIS movie search response did not contain movieList. keyword={}", cleanKeyword);
                return List.of();
            }
            int max = Math.max(1, Math.min(limit == null ? 10 : limit, 30));
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode movie : movieList) {
                if (results.size() >= max) {
                    break;
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("movieCd", text(movie, "movieCd"));
                result.put("movieNm", text(movie, "movieNm"));
                result.put("movieNmEn", text(movie, "movieNmEn"));
                result.put("prdtYear", text(movie, "prdtYear"));
                result.put("openDt", text(movie, "openDt"));
                result.put("genreAlt", text(movie, "genreAlt"));
                result.put("directors", peopleList(movie.path("directors")));
                result.put("companys", companyList(movie.path("companys")));
                results.add(result);
            }
            log.debug("KOBIS movie search completed. keyword={}, resultCount={}", cleanKeyword, results.size());
            return results;
        } catch (Exception ex) {
            log.warn("KOBIS movie search failed. keyword={}, baseUrl={}, reason={}", cleanKeyword, properties.baseUrl(), ex.toString());
            return List.of();
        }
    }

    public Optional<KobisMovieDetail> movieDetail(String movieCd) {
        String cleanMovieCd = clean(movieCd);
        if (!properties.hasApiKey() || cleanMovieCd == null) {
            if (!properties.hasApiKey()) {
                log.warn("KOBIS movie detail skipped because API key is not configured.");
            }
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                    .path("/movie/searchMovieInfo.json")
                    .queryParam("key", properties.apiKey())
                    .queryParam("movieCd", cleanMovieCd)
                    .encode()
                    .build()
                    .toUri();
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            JsonNode movieInfo = objectMapper.readTree(body).path("movieInfoResult").path("movieInfo");
            if (movieInfo.isMissingNode() || movieInfo.isNull()) {
                log.warn("KOBIS movie detail response did not contain movieInfo. movieCd={}", cleanMovieCd);
                return Optional.empty();
            }
            return Optional.of(new KobisMovieDetail(
                    text(movieInfo, "movieCd"),
                    text(movieInfo, "movieNm"),
                    text(movieInfo, "movieNmEn"),
                    text(movieInfo, "prdtYear"),
                    text(movieInfo, "openDt"),
                    genres(movieInfo.path("genres")),
                    credits(movieInfo.path("directors"), "DIRECTOR"),
                    credits(movieInfo.path("actors"), "ACTOR"),
                    credits(movieInfo.path("staffs"), "STAFF"),
                    body
            ));
        } catch (Exception ex) {
            log.warn("KOBIS movie detail failed. movieCd={}, baseUrl={}, reason={}", cleanMovieCd, properties.baseUrl(), ex.toString());
            return Optional.empty();
        }
    }

    private List<KobisCredit> credits(JsonNode nodes, String source) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<KobisCredit> credits = new ArrayList<>();
        for (JsonNode node : nodes) {
            credits.add(new KobisCredit(
                    text(node, "peopleNm"),
                    text(node, "peopleNmEn"),
                    "STAFF".equals(source) ? text(node, "staffRoleNm") : defaultRoleName(source),
                    source
            ));
        }
        return credits;
    }

    private List<Map<String, Object>> peopleList(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> people = new ArrayList<>();
        for (JsonNode node : nodes) {
            Map<String, Object> person = new LinkedHashMap<>();
            person.put("peopleNm", text(node, "peopleNm"));
            person.put("peopleNmEn", text(node, "peopleNmEn"));
            people.add(person);
        }
        return people;
    }

    private List<Map<String, Object>> companyList(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> companys = new ArrayList<>();
        for (JsonNode node : nodes) {
            Map<String, Object> company = new LinkedHashMap<>();
            company.put("companyCd", text(node, "companyCd"));
            company.put("companyNm", text(node, "companyNm"));
            companys.add(company);
        }
        return companys;
    }

    private String genres(JsonNode nodes) {
        if (!nodes.isArray()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (JsonNode node : nodes) {
            String name = text(node, "genreNm");
            if (name != null) {
                names.add(name);
            }
        }
        return names.isEmpty() ? null : names.stream().collect(Collectors.joining(", "));
    }

    private String defaultRoleName(String source) {
        if ("DIRECTOR".equals(source)) {
            return "감독";
        }
        if ("ACTOR".equals(source)) {
            return "배우";
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
