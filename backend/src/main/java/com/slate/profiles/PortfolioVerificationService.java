package com.slate.profiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.slate.operations.AuditLogService;
import com.slate.profiles.ProfileController.PortfolioItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PortfolioVerificationService {

    private static final String PROVIDER_KOBIS = "KOBIS";
    private static final String STATUS_NOT_VERIFIED = "NOT_VERIFIED";
    private static final String STATUS_ERROR = "ERROR";

    private final ProfileMapper profileMapper;
    private final KobisClient kobisClient;
    private final KobisRoleMatcher roleMatcher;
    private final AuditLogService auditLogService;

    public PortfolioVerificationService(
            ProfileMapper profileMapper,
            KobisClient kobisClient,
            KobisRoleMatcher roleMatcher,
            AuditLogService auditLogService
    ) {
        this.profileMapper = profileMapper;
        this.kobisClient = kobisClient;
        this.roleMatcher = roleMatcher;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> searchKobisMovies(String keyword, Integer limit) {
        return kobisClient.searchMovies(keyword, limit);
    }

    public void verifyAfterSave(Long userId, Long portfolioItemId, PortfolioItemRequest request) {
        try {
            String movieCd = kobisMovieCd(request);
            if (movieCd == null) {
                profileMapper.deletePortfolioVerification(portfolioItemId);
                return;
            }
            String creditName = clean(request.creditName());
            String roleName = clean(request.roleName());
            if (creditName == null || roleName == null) {
                upsert(baseVerification(portfolioItemId, request, STATUS_NOT_VERIFIED));
                return;
            }
            if (!kobisClient.hasApiKey()) {
                upsert(baseVerification(portfolioItemId, request, STATUS_ERROR));
                recordVerificationIssue(userId, portfolioItemId, "KOBIS_API_KEY_MISSING");
                return;
            }
            Optional<KobisMovieDetail> detail = kobisClient.movieDetail(movieCd);
            if (detail.isEmpty()) {
                upsert(baseVerification(portfolioItemId, request, STATUS_ERROR));
                recordVerificationIssue(userId, portfolioItemId, "KOBIS_MOVIE_DETAIL_EMPTY");
                return;
            }
            KobisMovieDetail movieDetail = detail.get();
            KobisVerificationMatch match = roleMatcher.match(movieDetail, creditName, roleName);
            Map<String, Object> verification = baseVerification(portfolioItemId, request, match.verificationStatus());
            verification.put("providerMovieCode", textOrDefault(movieDetail.movieCd(), movieCd));
            verification.put("providerMovieTitle", textOrDefault(movieDetail.movieNm(), request.kobisMovieNm()));
            verification.put("providerMovieTitleEn", textOrDefault(movieDetail.movieNmEn(), request.kobisMovieNmEn()));
            verification.put("providerMovieYear", textOrDefault(movieDetail.prdtYear(), request.kobisPrdtYear()));
            verification.put("providerOpenDate", textOrDefault(movieDetail.openDt(), request.kobisOpenDt()));
            verification.put("providerGenres", textOrDefault(movieDetail.genreAlt(), request.kobisGenreAlt()));
            verification.put("providerPersonName", match.providerPersonName());
            verification.put("providerPersonNameEn", match.providerPersonNameEn());
            verification.put("providerRoleName", match.providerRoleName());
            verification.put("matchedRoleGroup", match.matchedRoleGroup());
            verification.put("matchedSource", match.matchedSource());
            verification.put("rawResponseJson", movieDetail.rawResponseJson());
            upsert(verification);
        } catch (Exception ex) {
            try {
                upsert(baseVerification(portfolioItemId, request, STATUS_ERROR));
            } catch (Exception ignored) {
                // Verification persistence must never block the portfolio save.
            }
            recordVerificationIssue(userId, portfolioItemId, ex.getClass().getSimpleName());
        }
    }

    private void upsert(Map<String, Object> verification) {
        try {
            profileMapper.upsertPortfolioVerification(verification);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private Map<String, Object> baseVerification(Long portfolioItemId, PortfolioItemRequest request, String status) {
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("portfolioItemId", portfolioItemId);
        verification.put("provider", PROVIDER_KOBIS);
        verification.put("providerMovieCode", kobisMovieCd(request));
        verification.put("providerMovieTitle", textOrDefault(request.kobisMovieNm(), request.title()));
        verification.put("providerMovieTitleEn", clean(request.kobisMovieNmEn()));
        verification.put("providerMovieYear", clean(request.kobisPrdtYear()));
        verification.put("providerOpenDate", clean(request.kobisOpenDt()));
        verification.put("providerGenres", clean(request.kobisGenreAlt()));
        verification.put("providerPersonName", null);
        verification.put("providerPersonNameEn", null);
        verification.put("providerRoleName", null);
        verification.put("matchedRoleGroup", null);
        verification.put("matchedSource", null);
        verification.put("verificationStatus", status);
        verification.put("rawResponseJson", null);
        return verification;
    }

    private void recordVerificationIssue(Long userId, Long portfolioItemId, String reason) {
        try {
            auditLogService.recordOperation(
                    "WARN",
                    "KOBIS_VERIFICATION_ERROR",
                    "KOBIS 포트폴리오 검증을 완료하지 못했습니다.",
                    Map.of("userId", userId, "portfolioItemId", portfolioItemId, "reason", reason)
            );
        } catch (Exception ignored) {
            // Verification must never block portfolio save.
        }
    }

    private String kobisMovieCd(PortfolioItemRequest request) {
        String kobisMovieCd = clean(request.kobisMovieCd());
        if (kobisMovieCd != null) {
            return kobisMovieCd;
        }
        if (PROVIDER_KOBIS.equalsIgnoreCase(clean(request.externalSourceName()))) {
            return clean(request.externalReferenceId());
        }
        return null;
    }

    private String textOrDefault(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue == null ? clean(fallback) : cleanValue;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
