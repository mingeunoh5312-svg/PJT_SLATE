package com.slate.profiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.profiles.ProfileController.ProfileRequest;
import com.slate.profiles.ProfileController.PortfolioItemRequest;
import com.slate.profiles.ProfileController.PublicDataPortfolioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {

    private static final int MAX_PORTFOLIO_ITEMS = 20;
    private static final Set<String> PORTFOLIO_SOURCE_TYPES = Set.of("MANUAL", "PUBLIC_DATA", "PUBLIC_DATA_MANUAL");

    private final ProfileMapper profileMapper;
    private final AuditLogService auditLogService;
    private final PortfolioVerificationService portfolioVerificationService;

    public ProfileService(
            ProfileMapper profileMapper,
            AuditLogService auditLogService,
            PortfolioVerificationService portfolioVerificationService
    ) {
        this.profileMapper = profileMapper;
        this.auditLogService = auditLogService;
        this.portfolioVerificationService = portfolioVerificationService;
    }

    public Map<String, Object> byUserId(Long userId) {
        Map<String, Object> profile = profileMapper.selectProfileByUserId(userId);
        if (profile == null) {
            return null;
        }
        return enrich(profile);
    }

    public Map<String, Object> byProfileId(Long profileId) {
        Map<String, Object> profile = profileMapper.selectProfileById(profileId);
        if (profile == null) {
            throw new SlateException("프로필을 찾을 수 없습니다.");
        }
        return enrich(profile);
    }

    public Map<String, Object> publicByProfileId(Long profileId) {
        Map<String, Object> profile = profileMapper.selectPublicProfileById(profileId);
        if (profile == null) {
            throw new SlateException("공개 프로필을 찾을 수 없습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>(enrich(profile));
        result.remove("email");
        result.remove("deletedAt");
        return result;
    }

    public List<Map<String, Object>> myPortfolioItems(Long userId) {
        Map<String, Object> profile = ownProfile(userId);
        return normalizePortfolioItems(profileMapper.selectPortfolioItems(longValue(profile.get("profileId"))));
    }

    public List<Map<String, Object>> searchPublicData(String keyword, String itemType, Integer limit) {
        return profileMapper.selectPublicDataItems(textOrNull(keyword), textOrNull(itemType), safeLimit(limit, 30));
    }

    public List<Map<String, Object>> searchKobisMovies(String keyword, Integer limit) {
        return portfolioVerificationService.searchKobisMovies(keyword, safeLimit(limit, 10));
    }

    @Transactional
    public Map<String, Object> create(Long userId, ProfileRequest request) {
        if (profileMapper.selectProfileByUserId(userId) != null) {
            throw new SlateException("이미 프로필이 있습니다. 수정으로 진행하세요.");
        }
        Map<String, Object> deletedProfile = profileMapper.selectAnyProfileByUserId(userId);
        Map<String, Object> profile = requestMap(userId, null, request);
        if (deletedProfile != null) {
            Long profileId = longValue(deletedProfile.get("profileId"));
            profile.put("profileId", profileId);
            if (profileMapper.reactivateProfile(profile) == 0) {
                throw new SlateException("삭제된 프로필을 복구하지 못했습니다.");
            }
            replaceLinks(profileId, request);
            Map<String, Object> restored = byProfileId(profileId);
            auditLogService.recordAudit(userId, "PROFILE_RESTORED", "PROFILE", profileId, deletedProfile, restored);
            return restored;
        }
        profileMapper.insertProfile(profile);
        Long profileId = ((Number) profile.get("profileId")).longValue();
        replaceLinks(profileId, request);
        Map<String, Object> created = byProfileId(profileId);
        auditLogService.recordAudit(userId, "PROFILE_CREATED", "PROFILE", profileId, null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> createPortfolioItem(Long userId, PortfolioItemRequest request) {
        Map<String, Object> profile = ownProfile(userId);
        Long profileId = longValue(profile.get("profileId"));
        ensurePortfolioLimit(profileId);
        Map<String, Object> item = portfolioRequestMap(profileId, request);
        profileMapper.insertPortfolioItem(item);
        Long portfolioItemId = longValue(item.get("portfolioItemId"));
        portfolioVerificationService.verifyAfterSave(userId, portfolioItemId, request);
        Map<String, Object> created = profileMapper.selectPortfolioItemById(portfolioItemId);
        auditLogService.recordAudit(userId, "PORTFOLIO_ITEM_CREATED", "PORTFOLIO_ITEM", portfolioItemId, null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> createPortfolioItemFromPublicData(Long userId, PublicDataPortfolioRequest request) {
        Map<String, Object> profile = ownProfile(userId);
        Long profileId = longValue(profile.get("profileId"));
        ensurePortfolioLimit(profileId);
        Map<String, Object> publicData = profileMapper.selectPublicDataItemById(request.publicDataSyncItemId());
        if (publicData == null) {
            throw new SlateException("공공데이터 항목을 찾을 수 없습니다.");
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("profileId", profileId);
        item.put("publicDataSyncItemId", request.publicDataSyncItemId());
        item.put("title", textOrDefault(request.titleOverride(), stringValue(publicData.get("title"))));
        item.put("roleName", textOrNull(request.roleName()));
        item.put("creditName", null);
        item.put("description", textOrDefault(request.description(), stringValue(publicData.get("description"))));
        item.put("sourceType", "PUBLIC_DATA");
        item.put("externalSourceName", stringValue(publicData.get("sourceName")));
        item.put("externalReferenceId", stringValue(publicData.get("externalId")));
        item.put("url", validUrlOrNull(textOrDefault(request.url(), stringValue(publicData.get("providerUrl")))));
        item.put("thumbnailUrl", null);
        item.put("sortOrder", request.sortOrder() == null ? 0 : request.sortOrder());
        profileMapper.insertPortfolioItem(item);
        Long portfolioItemId = longValue(item.get("portfolioItemId"));
        Map<String, Object> created = profileMapper.selectPortfolioItemById(portfolioItemId);
        auditLogService.recordAudit(userId, "PORTFOLIO_ITEM_IMPORTED", "PORTFOLIO_ITEM", portfolioItemId, publicData, created);
        auditLogService.recordOperation(
                "INFO",
                "PORTFOLIO_PUBLIC_DATA_IMPORTED",
                "공공데이터 항목을 포트폴리오에 추가했습니다.",
                Map.of("userId", userId, "portfolioItemId", portfolioItemId, "publicDataSyncItemId", request.publicDataSyncItemId())
        );
        return created;
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long profileId, ProfileRequest request) {
        Map<String, Object> before = byProfileId(profileId);
        if (!userId.equals(longValue(before.get("userId")))) {
            throw new SlateException("수정할 수 있는 프로필을 찾을 수 없습니다.");
        }
        Map<String, Object> profile = requestMap(userId, profileId, request);
        if (profileMapper.updateProfile(profile) == 0) {
            throw new SlateException("수정할 수 있는 프로필을 찾을 수 없습니다.");
        }
        replaceLinks(profileId, request);
        Map<String, Object> updated = byProfileId(profileId);
        auditLogService.recordAudit(userId, "PROFILE_UPDATED", "PROFILE", profileId, before, updated);
        return updated;
    }

    @Transactional
    public Map<String, Object> deleteMyProfile(Long userId) {
        Map<String, Object> before = ownProfile(userId);
        Long profileId = longValue(before.get("profileId"));
        if (profileMapper.softDeleteProfile(userId, profileId) == 0) {
            throw new SlateException("삭제할 수 있는 프로필을 찾을 수 없습니다.");
        }
        Map<String, Object> deleted = new LinkedHashMap<>(before);
        deleted.put("visibility", "PRIVATE");
        deleted.put("activityStatus", "HIDDEN");
        deleted.put("profileCompletedYn", "N");
        deleted.put("status", "DELETED");
        auditLogService.recordAudit(userId, "PROFILE_DELETED", "PROFILE", profileId, before, deleted);
        auditLogService.recordOperation(
                "INFO",
                "PROFILE_DELETED",
                "사용자가 프로필을 소프트 삭제했습니다.",
                Map.of("userId", userId, "profileId", profileId)
        );
        return deleted;
    }

    @Transactional
    public Map<String, Object> updatePortfolioItem(Long userId, Long portfolioItemId, PortfolioItemRequest request) {
        Map<String, Object> before = profileMapper.selectOwnedPortfolioItem(userId, portfolioItemId);
        if (before == null) {
            throw new SlateException("수정할 수 있는 포트폴리오를 찾을 수 없습니다.");
        }
        Long profileId = longValue(before.get("profileId"));
        Map<String, Object> item = portfolioRequestMap(profileId, request);
        item.put("portfolioItemId", portfolioItemId);
        if (profileMapper.updatePortfolioItem(item) == 0) {
            throw new SlateException("포트폴리오를 수정하지 못했습니다.");
        }
        portfolioVerificationService.verifyAfterSave(userId, portfolioItemId, request);
        Map<String, Object> after = profileMapper.selectPortfolioItemById(portfolioItemId);
        auditLogService.recordAudit(userId, "PORTFOLIO_ITEM_UPDATED", "PORTFOLIO_ITEM", portfolioItemId, before, after);
        return after;
    }

    @Transactional
    public Map<String, Object> deletePortfolioItem(Long userId, Long portfolioItemId) {
        Map<String, Object> before = profileMapper.selectOwnedPortfolioItem(userId, portfolioItemId);
        if (before == null) {
            throw new SlateException("삭제할 수 있는 포트폴리오를 찾을 수 없습니다.");
        }
        if (profileMapper.deletePortfolioItem(userId, portfolioItemId) == 0) {
            throw new SlateException("포트폴리오를 삭제하지 못했습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>(before);
        result.put("status", "DELETED");
        auditLogService.recordAudit(userId, "PORTFOLIO_ITEM_DELETED", "PORTFOLIO_ITEM", portfolioItemId, before, result);
        return result;
    }

    private Map<String, Object> enrich(Map<String, Object> profile) {
        Long profileId = ((Number) profile.get("profileId")).longValue();
        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.putIfAbsent("profileImageUrl", null);
        result.put("roles", profileMapper.selectProfileRoles(profileId));
        result.put("genres", profileMapper.selectProfileGenres(profileId));
        result.put("collaborationConditions", profileMapper.selectProfileConditions(profileId));
        result.put("portfolioItems", normalizePortfolioItems(profileMapper.selectPortfolioItems(profileId)));
        return result;
    }

    private List<Map<String, Object>> normalizePortfolioItems(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>(row);
            result.putIfAbsent("uploadedThumbnailUrl", null);
            return result;
        }).toList();
    }

    private Map<String, Object> ownProfile(Long userId) {
        Map<String, Object> profile = profileMapper.selectProfileByUserId(userId);
        if (profile == null) {
            throw new SlateException("프로필을 먼저 생성해주세요.");
        }
        return profile;
    }

    private void ensurePortfolioLimit(Long profileId) {
        if (profileMapper.countActivePortfolioItems(profileId) >= MAX_PORTFOLIO_ITEMS) {
            throw new SlateException("포트폴리오는 최대 20개까지 등록할 수 있습니다.");
        }
    }

    private Map<String, Object> portfolioRequestMap(Long profileId, PortfolioItemRequest request) {
        String kobisMovieCd = textOrNull(request.kobisMovieCd());
        String sourceType = kobisMovieCd == null ? normalizeSourceType(request.sourceType()) : "PUBLIC_DATA_MANUAL";
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("profileId", profileId);
        item.put("publicDataSyncItemId", request.publicDataSyncItemId());
        item.put("title", request.title().trim());
        item.put("roleName", textOrNull(request.roleName()));
        item.put("creditName", textOrNull(request.creditName()));
        item.put("description", textOrNull(request.description()));
        item.put("sourceType", sourceType);
        item.put("externalSourceName", kobisMovieCd == null ? textOrNull(request.externalSourceName()) : "KOBIS");
        item.put("externalReferenceId", kobisMovieCd == null ? textOrNull(request.externalReferenceId()) : kobisMovieCd);
        item.put("url", validUrlOrNull(request.url()));
        item.put("thumbnailUrl", validUrlOrNull(request.thumbnailUrl()));
        item.put("sortOrder", request.sortOrder() == null ? 0 : request.sortOrder());
        return item;
    }

    private Map<String, Object> requestMap(Long userId, Long profileId, ProfileRequest request) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("profileId", profileId);
        profile.put("userId", userId);
        profile.put("displayName", request.displayName().trim());
        profile.put("shortIntro", request.shortIntro().trim());
        profile.put("detailIntro", textOrNull(request.detailIntro()));
        profile.put("visibility", request.visibility());
        profile.put("activityStatus", request.activityStatus());
        profile.put("regionId", request.regionId());
        profile.put("experienceLevel", request.experienceLevel());
        profile.put("joinAvailability", request.joinAvailability());
        profile.put("collaborationStatus", request.collaborationStatus());
        profile.put("travelRange", request.travelRange());
        profile.put("preferredDuration", request.preferredDuration());
        profile.put("equipmentStatus", textOrDefault(request.equipmentStatus(), "NOT_ENTERED"));
        profile.put("ageBand", textOrDefault(request.ageBand(), "PRIVATE"));
        profile.put("participationMode", textOrDefault(request.participationMode(), "HYBRID"));
        profile.put("profileCompletedYn", "Y");
        return profile;
    }

    private void replaceLinks(Long profileId, ProfileRequest request) {
        profileMapper.deleteProfileRoles(profileId);
        for (int i = 0; i < request.roleIds().size(); i++) {
            profileMapper.insertProfileRole(profileId, request.roleIds().get(i), i);
        }
        profileMapper.deleteProfileGenres(profileId);
        for (Long genreId : request.genreIds()) {
            profileMapper.insertProfileGenre(profileId, genreId);
        }
        profileMapper.deleteProfileConditions(profileId);
        for (String conditionCode : request.collaborationConditionCodes()) {
            profileMapper.insertProfileCondition(profileId, conditionCode);
        }
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String normalizeSourceType(String value) {
        String sourceType = textOrDefault(value, "MANUAL").toUpperCase();
        if (!PORTFOLIO_SOURCE_TYPES.contains(sourceType)) {
            throw new SlateException("지원하지 않는 포트폴리오 출처입니다.");
        }
        return sourceType;
    }

    private String validUrlOrNull(String value) {
        String url = textOrNull(value);
        if (url == null) {
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new SlateException("URL은 http 또는 https로 시작해야 합니다.");
        }
        return url;
    }

    private int safeLimit(Integer limit, int fallback) {
        return Math.max(1, Math.min(limit == null ? fallback : limit, 50));
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
