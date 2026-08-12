package com.slate.matching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.slate.common.SlateException;
import com.slate.matching.MatchingController.ApplicationRequest;
import com.slate.matching.MatchingController.BookmarkRequest;
import com.slate.matching.MatchingController.InvitationRequest;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MatchingService {

    private final MatchingMapper mapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public MatchingService(MatchingMapper mapper, NotificationService notificationService, AuditLogService auditLogService) {
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> teamToMembers(Long currentUserId, Map<String, Object> query) {
        Long slotId = longValue(query.get("slotId"));
        Long teamId = longValue(query.get("teamId"));
        Map<String, Object> slot = null;
        if (slotId != null) {
            if (teamId == null) {
                throw new SlateException("모집 역할을 사용하려면 기준 팀이 필요합니다.");
            }
            slot = mapper.selectSlotById(slotId);
            if (slot == null || !teamId.equals(longValue(slot.get("teamId")))) {
                throw new SlateException("선택한 팀의 모집 역할을 찾을 수 없습니다.");
            }
            if (!"OPEN".equals(str(slot, "recruitmentStatus"))
                    || !"OPEN".equals(str(slot, "slotStatus"))
                    || longValue(slot.get("remainingCount")) == null
                    || longValue(slot.get("remainingCount")) < 1L) {
                throw new SlateException("현재 모집 중인 역할만 사용할 수 있습니다.");
            }
        }
        Map<String, Object> enrichedTeam = null;
        if (teamId != null) {
            Map<String, Object> team = mapper.selectTeamById(teamId);
            if (team == null) {
                throw new SlateException("팀을 찾을 수 없습니다.");
            }
            String teamRole = mapper.selectActiveTeamRole(teamId, currentUserId);
            if (!"LEADER".equals(teamRole) && !"SUB_LEADER".equals(teamRole)) {
                throw new SlateException("팀장 또는 부팀장만 팀원을 찾을 수 있습니다.");
            }
            String teamStatus = str(team, "status");
            if (!Set.of("ACTIVE", "RECRUITING", "IN_PROGRESS", "RECRUITMENT_CLOSED").contains(teamStatus)) {
                throw new SlateException("활동 중인 팀만 기준 팀으로 사용할 수 있습니다.");
            }
            enrichedTeam = enrichTeam(team);
        }
        Map<String, Object> teamContext = enrichedTeam;
        Map<String, Object> slotContext = slot;
        List<Map<String, Object>> results = mapper.selectCandidateProfiles(teamId, slotId, currentUserId).stream()
                .map(this::enrichProfile)
                .filter(profile -> matchesTeamToMemberFilters(profile, slotContext, query))
                .map(profile -> teamContext == null
                        ? unscoredTeamMemberCandidate(profile)
                        : scoreTeamToMember(teamContext, slotContext, profile))
                .filter(row -> teamContext == null || isExposed(row))
                .sorted(teamContext == null ? profileUpdatedComparator() : matchComparator())
                .toList();
        return resultEnvelope(results, enrichedTeam, slot);
    }

    public Map<String, Object> memberToTeams(Long currentUserId, Map<String, Object> query) {
        Long profileId = longValue(query.get("profileId"));
        Map<String, Object> profile = profileId == null ? mapper.selectProfileByUserId(currentUserId) : mapper.selectProfileById(profileId);
        if (profile == null) {
            throw new SlateException("매칭에 사용할 프로필이 필요합니다.");
        }
        Map<String, Object> enrichedProfile = enrichProfile(profile);
        Long profileUserId = longValue(enrichedProfile.get("userId"));
        List<Map<String, Object>> results = mapper.selectOpenRecruitmentSlots(currentUserId).stream()
                .filter(slot -> !mapper.existsActiveTeamMember(longValue(slot.get("teamId")), profileUserId))
                .filter(slot -> matchesMemberToTeamFilters(enrichedProfile, slot, query))
                .map(slot -> scoreMemberToTeam(enrichedProfile, enrichSlotTeam(slot)))
                .filter(this::isExposed)
                .sorted(matchComparator())
                .toList();
        return resultEnvelope(results, enrichedProfile, null);
    }

    public Map<String, Object> activePolicy() {
        Map<String, Object> policy = mapper.selectActiveScorePolicy();
        if (policy == null) {
            return fallbackPolicy();
        }
        List<Map<String, Object>> items = mapper.selectScorePolicyItems(longValue(policy.get("policyId")));
        Map<String, Object> result = new LinkedHashMap<>(policy);
        result.put("finalRatio", weights(items, "FINAL_RATIO"));
        result.put("firstFilterWeights", weights(items, "FIRST_FILTER"));
        result.put("internalWeights", weights(items, "INTERNAL"));
        result.put("items", items);
        return result;
    }

    public Map<String, Object> previewPolicyImpact(List<Map<String, Object>> proposedItems, int limit) {
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 30));
        ScorePolicy currentPolicy = scorePolicy();
        ScorePolicy proposedPolicy = scorePolicy(proposedItems);
        List<Map<String, Object>> comparisons = new ArrayList<>();
        int scannedPairs = 0;

        slotLoop:
        for (Map<String, Object> slotRow : mapper.selectOpenRecruitmentSlots(null)) {
            Long teamId = longValue(slotRow.get("teamId"));
            Map<String, Object> team = teamId == null ? null : mapper.selectTeamById(teamId);
            if (team == null) {
                continue;
            }
            Map<String, Object> enrichedTeam = enrichTeam(team);
            Map<String, Object> slot = new LinkedHashMap<>(slotRow);
            for (Map<String, Object> rawProfile : mapper.selectCandidateProfiles(teamId, longValue(slot.get("slotId")), null)) {
                Map<String, Object> profile = enrichProfile(rawProfile);
                if (!matchesTeamToMemberFilters(profile, slot, Map.of())) {
                    continue;
                }
                scannedPairs++;
                Map<String, Object> before = scoreTeamToMember(enrichedTeam, slot, profile, currentPolicy);
                Map<String, Object> after = scoreTeamToMember(enrichedTeam, slot, profile, proposedPolicy);
                double beforeScore = doubleValue(before.get("score"));
                double afterScore = doubleValue(after.get("score"));
                if (beforeScore < 40.0 && afterScore < 40.0 && Math.abs(afterScore - beforeScore) < 0.1) {
                    continue;
                }
                Map<String, Object> comparison = new LinkedHashMap<>();
                comparison.put("teamId", teamId);
                comparison.put("teamName", enrichedTeam.get("name"));
                comparison.put("slotId", slot.get("slotId"));
                comparison.put("recruitmentTitle", slot.get("recruitmentTitle"));
                comparison.put("roleName", slot.get("roleName"));
                comparison.put("profileId", profile.get("profileId"));
                comparison.put("profileUserId", profile.get("userId"));
                comparison.put("profileName", defaultString(profile.get("displayName"), defaultString(profile.get("nickname"), "후보")));
                comparison.put("beforeScore", beforeScore);
                comparison.put("afterScore", afterScore);
                comparison.put("delta", round(afterScore - beforeScore));
                comparison.put("beforeExposureType", exposureType(beforeScore));
                comparison.put("afterExposureType", exposureType(afterScore));
                comparison.put("beforeReasons", before.get("reasons"));
                comparison.put("afterReasons", after.get("reasons"));
                comparisons.add(comparison);
                if (scannedPairs >= 500) {
                    break slotLoop;
                }
            }
        }

        comparisons.sort(Comparator
                .<Map<String, Object>, Double>comparing(row -> Math.abs(doubleValue(row.get("delta"))))
                .reversed()
                .thenComparing(Comparator.<Map<String, Object>, Double>comparing(row -> doubleValue(row.get("afterScore"))).reversed()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", previewSummary(comparisons, scannedPairs));
        result.put("samples", comparisons.stream().limit(safeLimit).toList());
        result.put("currentPolicy", activePolicy());
        result.put("proposedWeights", Map.of(
                "finalRatio", weights(proposedItems, "FINAL_RATIO"),
                "firstFilterWeights", weights(proposedItems, "FIRST_FILTER"),
                "internalWeights", weights(proposedItems, "INTERNAL")
        ));
        return result;
    }

    @Transactional
    public Map<String, Object> bookmark(Long userId, BookmarkRequest request) {
        String targetType = Objects.toString(request.targetType(), "").trim().toUpperCase();
        if (!Set.of("PROFILE", "TEAM", "RECRUITMENT").contains(targetType)) {
            throw new SlateException("저장 대상은 PROFILE, TEAM, RECRUITMENT만 가능합니다.");
        }
        int inserted = mapper.insertBookmark(userId, targetType, request.targetId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saved", true);
        result.put("created", inserted > 0);
        result.put("alreadySaved", inserted == 0);
        result.put("targetType", targetType);
        result.put("targetId", request.targetId());
        if (inserted > 0) {
            mapper.insertActionLog(userId, targetType + "_BOOKMARK", targetType, request.targetId(), null, null);
            auditLogService.recordAudit(userId, targetType + "_BOOKMARK_CREATED", targetType, request.targetId(), null, result);
        }
        return result;
    }

    public List<Map<String, Object>> bookmarks(Long userId, String requestedTargetType) {
        String targetType = normalizeBookmarkType(requestedTargetType);
        if (!"TEAM".equals(targetType)) {
            throw new SlateException("현재 저장 목록은 TEAM만 조회할 수 있습니다.");
        }
        return mapper.selectTeamBookmarks(userId).stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>(row);
            Long teamId = longValue(row.get("teamId"));
            result.put("genres", teamId == null ? List.of() : mapper.selectTeamGenres(teamId));
            result.put("openRoles", teamId == null ? List.of() : mapper.selectOpenRecruitmentSlotsByTeamId(teamId, userId));
            result.put("savedByCurrentUser", true);
            return result;
        }).toList();
    }

    @Transactional
    public Map<String, Object> deleteBookmark(Long userId, String requestedTargetType, Long targetId) {
        String targetType = normalizeBookmarkType(requestedTargetType);
        int deleted = mapper.deleteBookmark(userId, targetType, targetId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saved", false);
        result.put("removed", deleted > 0);
        result.put("targetType", targetType);
        result.put("targetId", targetId);
        if (deleted > 0) {
            mapper.insertActionLog(userId, targetType + "_BOOKMARK_REMOVED", targetType, targetId, null, null);
            auditLogService.recordAudit(userId, targetType + "_BOOKMARK_REMOVED", targetType, targetId, Map.of("saved", true), result);
        }
        return result;
    }

    private String normalizeBookmarkType(String targetType) {
        String normalized = Objects.toString(targetType, "").trim().toUpperCase();
        if (!Set.of("PROFILE", "TEAM", "RECRUITMENT").contains(normalized)) {
            throw new SlateException("저장 대상은 PROFILE, TEAM, RECRUITMENT만 가능합니다.");
        }
        return normalized;
    }

    @Transactional
    public Map<String, Object> invite(Long inviterUserId, InvitationRequest request) {
        assertLeaderOrSubLeader(inviterUserId, request.teamId());
        Map<String, Object> slot = requireOpenSlot(request.teamId(), request.recruitmentId(), request.slotId());
        if (mapper.countPendingInvitation(request.teamId(), request.slotId(), request.targetUserId()) > 0) {
            throw new SlateException("이미 대기 중인 초대가 있습니다.");
        }
        if (mapper.insertInvitation(request.teamId(), request.recruitmentId(), request.slotId(), request.targetUserId(), inviterUserId, request.message()) == 0) {
            throw new SlateException("이미 대기 중인 초대가 있습니다.");
        }
        mapper.insertActionLog(inviterUserId, "INVITE_SENT", "INVITATION", request.targetUserId(), request.teamId(), roleIdOfSlot(request.slotId()));
        Map<String, Object> team = mapper.selectTeamById(request.teamId());
        notificationService.send(
                request.targetUserId(),
                inviterUserId,
                "TEAM",
                "팀 초대가 도착했습니다.",
                defaultString(team == null ? null : team.get("name"), "Slate 팀") + "에서 함께 작업하자는 초대를 보냈습니다.",
                "TEAM",
                request.teamId()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", true);
        result.put("teamId", request.teamId());
        result.put("slotId", request.slotId());
        result.put("targetUserId", request.targetUserId());
        Map<String, Object> invitation = mapper.selectPendingInvitation(request.teamId(), request.slotId(), request.targetUserId());
        if (invitation != null) {
            result.put("invitationId", invitation.get("invitationId"));
        }
        auditLogService.recordAudit(inviterUserId, "MATCHING_INVITATION_SENT", "TEAM", request.teamId(), null, result);
        return result;
    }

    public List<Map<String, Object>> sentInvitations(Long userId) {
        return mapper.selectSentInvitations(userId);
    }

    @Transactional
    public Map<String, Object> cancelInvitation(Long userId, Long invitationId) {
        Map<String, Object> before = mapper.selectInvitationById(invitationId);
        if (before == null) {
            throw new SlateException("초대를 찾을 수 없습니다.");
        }
        assertLeaderOrSubLeader(userId, longValue(before.get("teamId")));
        int updated = mapper.cancelPendingInvitation(invitationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canceled", updated > 0);
        result.put("invitationId", invitationId);
        result.put("teamId", before.get("teamId"));
        result.put("slotId", before.get("slotId"));
        result.put("targetUserId", before.get("targetUserId"));
        if (updated > 0) {
            mapper.insertActionLog(userId, "INVITE_CANCELED", "INVITATION", invitationId, longValue(before.get("teamId")), roleIdOfSlot(longValue(before.get("slotId"))));
            auditLogService.recordAudit(userId, "MATCHING_INVITATION_CANCELED", "TEAM_INVITATION", invitationId, before, result);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> apply(Long applicantUserId, ApplicationRequest request) {
        Map<String, Object> slot = requireOpenSlot(request.teamId(), request.recruitmentId(), request.slotId());
        if (mapper.countPendingApplication(request.teamId(), request.slotId(), applicantUserId) > 0) {
            throw new SlateException("이미 대기 중인 지원이 있습니다.");
        }
        if (mapper.insertApplication(request.teamId(), request.recruitmentId(), request.slotId(), applicantUserId, request.message()) == 0) {
            throw new SlateException("이미 대기 중인 지원이 있습니다.");
        }
        mapper.insertActionLog(applicantUserId, "APPLICATION_SENT", "APPLICATION", request.teamId(), request.teamId(), roleIdOfSlot(request.slotId()));
        notificationService.sendToTeamManagers(
                request.teamId(),
                applicantUserId,
                "새 팀 지원이 도착했습니다.",
                defaultString(slot == null ? null : slot.get("roleName"), "모집 역할") + " 슬롯에 새 지원이 등록되었습니다.",
                "TEAM",
                request.teamId()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", true);
        result.put("teamId", request.teamId());
        result.put("slotId", request.slotId());
        Map<String, Object> application = mapper.selectPendingApplication(request.teamId(), request.slotId(), applicantUserId);
        if (application != null) {
            result.put("applicationId", application.get("applicationId"));
        }
        auditLogService.recordAudit(applicantUserId, "MATCHING_APPLICATION_SENT", "TEAM", request.teamId(), null, result);
        return result;
    }

    public List<Map<String, Object>> sentApplications(Long userId) {
        return mapper.selectSentApplications(userId);
    }

    @Transactional
    public Map<String, Object> cancelApplication(Long userId, Long applicationId) {
        Map<String, Object> before = mapper.selectApplicationById(applicationId);
        if (before == null) {
            throw new SlateException("지원을 찾을 수 없습니다.");
        }
        if (!Objects.equals(longValue(before.get("applicantUserId")), userId)) {
            throw new SlateException("본인이 보낸 지원만 취소할 수 있습니다.");
        }
        int updated = mapper.cancelPendingApplication(applicationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("canceled", updated > 0);
        result.put("applicationId", applicationId);
        result.put("teamId", before.get("teamId"));
        result.put("slotId", before.get("slotId"));
        if (updated > 0) {
            mapper.insertActionLog(userId, "APPLICATION_CANCELED", "APPLICATION", applicationId, longValue(before.get("teamId")), roleIdOfSlot(longValue(before.get("slotId"))));
            auditLogService.recordAudit(userId, "MATCHING_APPLICATION_CANCELED", "TEAM_APPLICATION", applicationId, before, result);
        }
        return result;
    }

    private Map<String, Object> scoreTeamToMember(Map<String, Object> team, Map<String, Object> slot, Map<String, Object> profile) {
        return scoreTeamToMember(team, slot, profile, scorePolicy());
    }

    private Map<String, Object> unscoredTeamMemberCandidate(Map<String, Object> profile) {
        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.put("score", null);
        result.put("scoreBadge", "선택된 팀이 없습니다.");
        result.put("exposureType", "PRIMARY");
        result.put("reasons", List.of("필터 조건 기준"));
        result.put("actions", Map.of("canViewDetail", true, "canInvite", false, "canSave", true));
        return result;
    }

    private Map<String, Object> scoreTeamToMember(Map<String, Object> team, Map<String, Object> slot, Map<String, Object> profile, ScorePolicy policy) {
        Set<Long> profileRoleIds = idSet(list(profile.get("roles")), "roleId");
        Set<Long> profileGenreIds = idSet(list(profile.get("genres")), "genreId");
        Set<Long> teamGenreIds = idSet(list(team.get("genres")), "genreId");
        Set<String> profileConditions = stringSet(list(profile.get("collaborationConditions")).stream().map(row -> row.get("conditionCode")).toList());

        Long slotRoleId = slot == null ? null : longValue(slot.get("roleId"));
        String requiredExperience = slot == null ? str(profile, "experienceLevel") : str(slot, "requiredExperienceLevel");
        String requiredCondition = slot == null ? "ANY" : str(slot, "collaborationCondition");
        String roleDuration = slot == null ? str(team, "expectedDuration") : defaultString(slot.get("roleDuration"), str(team, "expectedDuration"));

        ScoreParts parts = new ScoreParts(
                slotRoleId == null || profileRoleIds.contains(slotRoleId) ? 1.0 : 0.25,
                distanceScore(team, profile, str(profile, "travelRange")),
                joinScore(str(profile, "joinAvailability")),
                conditionScore(requiredCondition, profileConditions),
                intersects(profileGenreIds, teamGenreIds) ? 1.0 : 0.3,
                experienceScore(requiredExperience, str(profile, "experienceLevel")),
                collaborationStatusScore(str(profile, "collaborationStatus")),
                travelRangeScore(str(profile, "travelRange")),
                durationScore(str(profile, "preferredDuration"), roleDuration)
        );
        double score = finalScore(policy, parts);
        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.put("score", round(score));
        result.put("scoreBadge", scoreBadge(score));
        result.put("exposureType", score >= 50.0 ? "PRIMARY" : "SUPPLEMENTARY");
        result.put("reasons", reasons(parts));
        result.put("actions", Map.of("canViewDetail", true, "canInvite", true, "canSave", true));
        return result;
    }

    private Map<String, Object> scoreMemberToTeam(Map<String, Object> profile, Map<String, Object> slot) {
        return scoreMemberToTeam(profile, slot, scorePolicy());
    }

    private Map<String, Object> scoreMemberToTeam(Map<String, Object> profile, Map<String, Object> slot, ScorePolicy policy) {
        Set<Long> profileRoleIds = idSet(list(profile.get("roles")), "roleId");
        Set<Long> profileGenreIds = idSet(list(profile.get("genres")), "genreId");
        Set<Long> teamGenreIds = idSet(list(slot.get("teamGenres")), "genreId");
        Set<String> profileConditions = stringSet(list(profile.get("collaborationConditions")).stream().map(row -> row.get("conditionCode")).toList());
        ScoreParts parts = new ScoreParts(
                profileRoleIds.contains(longValue(slot.get("roleId"))) ? 1.0 : 0.25,
                distanceScore(slot, profile, str(profile, "travelRange")),
                joinScore(str(profile, "joinAvailability")),
                conditionScore(str(slot, "collaborationCondition"), profileConditions),
                intersects(profileGenreIds, teamGenreIds) ? 1.0 : 0.3,
                experienceScore(str(slot, "requiredExperienceLevel"), str(profile, "experienceLevel")),
                collaborationStatusScore(str(profile, "collaborationStatus")),
                travelRangeScore(str(profile, "travelRange")),
                durationScore(str(profile, "preferredDuration"), defaultString(slot.get("roleDuration"), str(slot, "expectedDuration")))
        );
        double score = finalScore(policy, parts);
        Map<String, Object> result = new LinkedHashMap<>(slot);
        result.put("score", round(score));
        result.put("scoreBadge", scoreBadge(score));
        result.put("exposureType", score >= 50.0 ? "PRIMARY" : "SUPPLEMENTARY");
        result.put("reasons", reasons(parts));
        result.put("actions", Map.of("canViewDetail", true, "canApply", true, "canSave", true));
        return result;
    }

    private boolean matchesTeamToMemberFilters(Map<String, Object> profile, Map<String, Object> slot, Map<String, Object> query) {
        Set<Long> roleFilter = longSet(query.get("roleIds"));
        Set<Long> profileRoleIds = idSet(list(profile.get("roles")), "roleId");
        if (!roleFilter.isEmpty() && !intersects(roleFilter, profileRoleIds)) {
            return false;
        }
        if (roleFilter.isEmpty() && slot != null && !profileRoleIds.contains(longValue(slot.get("roleId")))) {
            return false;
        }
        Set<Long> genreFilter = longSet(query.get("genreIds"));
        if (!genreFilter.isEmpty() && !intersects(genreFilter, idSet(list(profile.get("genres")), "genreId"))) {
            return false;
        }
        if (!matchesRegionFilter(profile, query)) {
            return false;
        }
        Set<String> experienceFilter = stringSet(query.get("experienceLevel"));
        if (!experienceFilter.isEmpty() && !experienceFilter.contains(str(profile, "experienceLevel"))) {
            return false;
        }
        Set<String> joinAvailabilityFilter = stringSet(query.get("joinAvailability"));
        if (!joinAvailabilityFilter.isEmpty() && !joinAvailabilityFilter.contains(str(profile, "joinAvailability"))) {
            return false;
        }
        Set<String> conditionFilter = stringSet(query.get("collaborationCondition"));
        if (conditionFilter.isEmpty()) {
            return true;
        }
        Set<String> profileConditions = stringSet(list(profile.get("collaborationConditions")).stream().map(row -> row.get("conditionCode")).toList());
        return conditionFilter.contains("ANY") || profileConditions.contains("ANY") || intersects(conditionFilter, profileConditions);
    }

    private boolean matchesMemberToTeamFilters(Map<String, Object> profile, Map<String, Object> slot, Map<String, Object> query) {
        Set<Long> roleFilter = longSet(query.get("roleIds"));
        Long slotRoleId = longValue(slot.get("roleId"));
        if (!roleFilter.isEmpty() && !roleFilter.contains(slotRoleId)) {
            return false;
        }
        if (roleFilter.isEmpty() && !idSet(list(profile.get("roles")), "roleId").contains(slotRoleId)) {
            return false;
        }
        Set<Long> genreFilter = longSet(query.get("genreIds"));
        if (!genreFilter.isEmpty() && !intersects(genreFilter, idSet(list(slot.get("teamGenres")), "genreId"))) {
            return false;
        }
        if (!matchesRegionFilter(slot, query)) {
            return false;
        }
        Set<String> experienceFilter = stringSet(query.get("experienceLevel"));
        if (!experienceFilter.isEmpty() && !experienceFilter.contains(str(slot, "requiredExperienceLevel"))) {
            return false;
        }
        Set<String> conditionFilter = stringSet(query.get("collaborationCondition"));
        return conditionFilter.isEmpty() || conditionFilter.contains("ANY") || conditionFilter.contains(str(slot, "collaborationCondition"));
    }

    private boolean matchesRegionFilter(Map<String, Object> row, Map<String, Object> query) {
        Set<Long> regionFilter = longSet(query.get("regionIds"));
        Set<String> sidoFilter = stringSet(query.get("regionSidos"));
        if (regionFilter.isEmpty() && sidoFilter.isEmpty()) {
            return true;
        }
        Long regionId = longValue(row.get("regionId"));
        if (regionId != null && regionFilter.contains(regionId)) {
            return true;
        }
        String sidoName = str(row, "sidoName").trim();
        return StringUtils.hasText(sidoName) && sidoFilter.contains(sidoName);
    }

    private Map<String, Object> enrichProfile(Map<String, Object> profile) {
        Long profileId = longValue(profile.get("profileId"));
        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.put("roles", mapper.selectProfileRoles(profileId));
        result.put("genres", mapper.selectProfileGenres(profileId));
        result.put("collaborationConditions", mapper.selectProfileConditions(profileId));
        result.put("portfolioItems", mapper.selectProfilePortfolioItems(profileId));
        return result;
    }

    private Map<String, Object> enrichTeam(Map<String, Object> team) {
        Long teamId = longValue(team.get("teamId"));
        Map<String, Object> result = new LinkedHashMap<>(team);
        result.put("genres", mapper.selectTeamGenres(teamId));
        return result;
    }

    private Map<String, Object> enrichSlotTeam(Map<String, Object> slot) {
        Map<String, Object> result = new LinkedHashMap<>(slot);
        result.put("teamGenres", mapper.selectTeamGenres(longValue(slot.get("teamId"))));
        return result;
    }

    private Map<String, Object> resultEnvelope(List<Map<String, Object>> results, Map<String, Object> context, Map<String, Object> slot) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primary", results.stream().filter(row -> "PRIMARY".equals(row.get("exposureType"))).toList());
        result.put("supplementary", results.stream().filter(row -> "SUPPLEMENTARY".equals(row.get("exposureType"))).toList());
        result.put("totalCount", results.size());
        result.put("context", context);
        result.put("slot", slot);
        result.put("policy", activePolicy());
        return result;
    }

    private Map<String, Double> weights(List<Map<String, Object>> items, String group) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            if (group.equals(str(item, "scoreGroup"))) {
                result.put(str(item, "elementCode"), doubleValue(item.get("weight")));
            }
        }
        return result;
    }

    private ScorePolicy scorePolicy() {
        Map<String, Object> policy = activePolicy();
        return new ScorePolicy(doubleMap(policy.get("finalRatio")), doubleMap(policy.get("firstFilterWeights")), doubleMap(policy.get("internalWeights")));
    }

    private ScorePolicy scorePolicy(List<Map<String, Object>> items) {
        return new ScorePolicy(weights(items, "FINAL_RATIO"), weights(items, "FIRST_FILTER"), weights(items, "INTERNAL"));
    }

    private double finalScore(ScorePolicy policy, ScoreParts parts) {
        double first = parts.role * policy.first("role")
                + parts.regionDistance * policy.first("region_distance")
                + parts.joinTime * policy.first("join_time")
                + parts.collaborationCondition * policy.first("collaboration_condition")
                + parts.genre * policy.first("genre")
                + parts.experience * policy.first("experience");
        double internal = parts.collaborationStatus * policy.internal("collaboration_status")
                + parts.travelRange * policy.internal("travel_range")
                + parts.durationFit * policy.internal("duration_fit");
        return first * (policy.finalRatio("first_filter") / 100.0)
                + internal * (policy.finalRatio("internal") / 100.0);
    }

    private Map<String, Object> previewSummary(List<Map<String, Object>> comparisons, int scannedPairs) {
        double beforeAverage = comparisons.stream().mapToDouble(row -> doubleValue(row.get("beforeScore"))).average().orElse(0.0);
        double afterAverage = comparisons.stream().mapToDouble(row -> doubleValue(row.get("afterScore"))).average().orElse(0.0);
        long promotedToPrimary = comparisons.stream()
                .filter(row -> !"PRIMARY".equals(row.get("beforeExposureType")) && "PRIMARY".equals(row.get("afterExposureType")))
                .count();
        long demotedFromPrimary = comparisons.stream()
                .filter(row -> "PRIMARY".equals(row.get("beforeExposureType")) && !"PRIMARY".equals(row.get("afterExposureType")))
                .count();
        long newlyExposed = comparisons.stream()
                .filter(row -> "HIDDEN".equals(row.get("beforeExposureType")) && !"HIDDEN".equals(row.get("afterExposureType")))
                .count();
        long hiddenAfter = comparisons.stream()
                .filter(row -> !"HIDDEN".equals(row.get("beforeExposureType")) && "HIDDEN".equals(row.get("afterExposureType")))
                .count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scannedPairs", scannedPairs);
        summary.put("shownPairs", comparisons.size());
        summary.put("beforeAverage", round(beforeAverage));
        summary.put("afterAverage", round(afterAverage));
        summary.put("averageDelta", round(afterAverage - beforeAverage));
        summary.put("beforePrimary", comparisons.stream().filter(row -> "PRIMARY".equals(row.get("beforeExposureType"))).count());
        summary.put("afterPrimary", comparisons.stream().filter(row -> "PRIMARY".equals(row.get("afterExposureType"))).count());
        summary.put("promotedToPrimary", promotedToPrimary);
        summary.put("demotedFromPrimary", demotedFromPrimary);
        summary.put("newlyExposed", newlyExposed);
        summary.put("hiddenAfter", hiddenAfter);
        return summary;
    }

    private void assertLeaderOrSubLeader(Long userId, Long teamId) {
        String role = mapper.selectActiveTeamRole(teamId, userId);
        if (!"LEADER".equals(role) && !"SUB_LEADER".equals(role)) {
            throw new SlateException("팀장 또는 부팀장만 초대할 수 있습니다.");
        }
    }

    private Map<String, Object> requireOpenSlot(Long teamId, Long recruitmentId, Long slotId) {
        Map<String, Object> slot = mapper.selectSlotById(slotId);
        if (slot == null
                || !Objects.equals(longValue(slot.get("teamId")), teamId)
                || !Objects.equals(longValue(slot.get("recruitmentId")), recruitmentId)
                || !"OPEN".equals(slot.get("slotStatus"))
                || !"OPEN".equals(slot.get("recruitmentStatus"))) {
            throw new SlateException("모집 중인 슬롯에만 요청할 수 있습니다.");
        }
        return slot;
    }

    private Long roleIdOfSlot(Long slotId) {
        Map<String, Object> slot = mapper.selectSlotById(slotId);
        return slot == null ? null : longValue(slot.get("roleId"));
    }

    private Map<String, Object> fallbackPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyId", null);
        policy.put("policyName", "기본 fallback 정책");
        policy.put("version", 0);
        policy.put("finalRatio", new LinkedHashMap<>(Map.of("first_filter", 80.0, "internal", 20.0)));
        Map<String, Double> first = new LinkedHashMap<>();
        first.put("role", 35.0);
        first.put("region_distance", 20.0);
        first.put("join_time", 15.0);
        first.put("collaboration_condition", 15.0);
        first.put("genre", 10.0);
        first.put("experience", 5.0);
        policy.put("firstFilterWeights", first);
        Map<String, Double> internal = new LinkedHashMap<>();
        internal.put("collaboration_status", 40.0);
        internal.put("travel_range", 35.0);
        internal.put("duration_fit", 25.0);
        policy.put("internalWeights", internal);
        return policy;
    }

    private boolean isExposed(Map<String, Object> row) {
        return doubleValue(row.get("score")) >= 40.0;
    }

    private Comparator<Map<String, Object>> matchComparator() {
        return Comparator.<Map<String, Object>, Double>comparing(row -> doubleValue(row.get("score"))).reversed();
    }

    private Comparator<Map<String, Object>> profileUpdatedComparator() {
        return (left, right) -> {
            int byUpdated = Objects.toString(right.get("updatedAt"), "")
                    .compareTo(Objects.toString(left.get("updatedAt"), ""));
            if (byUpdated != 0) {
                return byUpdated;
            }
            return Long.compare(defaultLong(right.get("profileId")), defaultLong(left.get("profileId")));
        };
    }

    private long defaultLong(Object value) {
        Long parsed = longValue(value);
        return parsed == null ? 0L : parsed;
    }

    private List<String> reasons(ScoreParts parts) {
        List<String> reasons = new ArrayList<>();
        if (parts.role >= 1.0) reasons.add("역할 일치");
        if (parts.regionDistance >= 0.8) reasons.add("지역 가까움");
        if (parts.joinTime >= 0.8) reasons.add("일정 적합");
        if (parts.collaborationCondition >= 0.8) reasons.add("협업 조건 일치");
        if (parts.genre >= 1.0) reasons.add("장르 일치");
        if (parts.experience >= 0.8) reasons.add("경력 적합");
        if (parts.collaborationStatus >= 0.75) reasons.add("협업 가능");
        if (parts.durationFit >= 0.8) reasons.add("작업 기간 적합");
        return reasons.stream().distinct().limit(4).toList();
    }

    private String scoreBadge(double score) {
        if (score >= 80.0) return "높은 적합도";
        if (score >= 50.0) return "적합";
        if (score >= 40.0) return "조건 조정 필요";
        return "";
    }

    private String exposureType(double score) {
        if (score >= 50.0) return "PRIMARY";
        if (score >= 40.0) return "SUPPLEMENTARY";
        return "HIDDEN";
    }

    private double distanceScore(Map<String, Object> team, Map<String, Object> profile, String travelRange) {
        if ("Y".equals(str(team, "regionAnyYn")) || "ANYWHERE".equals(travelRange)) {
            return 1.0;
        }
        Double teamLat = doubleOrNull(team.get("centerLat"));
        Double teamLng = doubleOrNull(team.get("centerLng"));
        Double profileLat = doubleOrNull(profile.get("centerLat"));
        Double profileLng = doubleOrNull(profile.get("centerLng"));
        if (teamLat == null || teamLng == null || profileLat == null || profileLng == null) {
            return 0.7;
        }
        double distance = haversineKm(teamLat, teamLng, profileLat, profileLng);
        double allowedKm = switch (defaultString(travelRange, "KM_30")) {
            case "KM_10" -> 10.0;
            case "KM_100" -> 100.0;
            case "ANYWHERE" -> 10000.0;
            default -> 30.0;
        };
        if (distance <= allowedKm) return 1.0;
        if (distance <= allowedKm * 2.0) return 0.6;
        return 0.2;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double radius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double joinScore(String joinAvailability) {
        return switch (defaultString(joinAvailability, "NEGOTIABLE")) {
            case "IMMEDIATE", "WITHIN_1W" -> 1.0;
            case "WITHIN_2W", "NEGOTIABLE" -> 0.85;
            case "WITHIN_1M" -> 0.65;
            default -> 0.35;
        };
    }

    private double conditionScore(String requiredCondition, Set<String> profileConditions) {
        if ("ANY".equals(requiredCondition) || profileConditions.contains("ANY")) return 0.85;
        if (profileConditions.contains(requiredCondition)) return 1.0;
        if (profileConditions.contains("NEGOTIABLE") || "NEGOTIABLE".equals(requiredCondition)) return 0.75;
        return 0.25;
    }

    private double collaborationStatusScore(String status) {
        return switch (defaultString(status, "CONSIDERING")) {
            case "AVAILABLE" -> 1.0;
            case "CONSIDERING" -> 0.75;
            default -> 0.2;
        };
    }

    private double travelRangeScore(String travelRange) {
        return switch (defaultString(travelRange, "KM_30")) {
            case "ANYWHERE" -> 1.0;
            case "KM_100" -> 0.85;
            case "KM_30" -> 0.7;
            default -> 0.5;
        };
    }

    private double experienceScore(String required, String actual) {
        int requiredRank = experienceRank(required);
        int actualRank = experienceRank(actual);
        if (actualRank >= requiredRank) return 1.0;
        if (actualRank + 1 == requiredRank) return 0.6;
        return 0.25;
    }

    private int experienceRank(String code) {
        return switch (defaultString(code, "Y0_3")) {
            case "Y10_PLUS" -> 3;
            case "Y3_10" -> 2;
            default -> 1;
        };
    }

    private double durationScore(String preferred, String expected) {
        String preferredCode = defaultString(preferred, "ANY");
        String expectedCode = defaultString(expected, "ANY");
        if ("ANY".equals(preferredCode) || "ANY".equals(expectedCode)) return 0.85;
        if (preferredCode.equals(expectedCode)) return 1.0;
        return durationRank(preferredCode) >= durationRank(expectedCode) ? 0.9 : 0.45;
    }

    private int durationRank(String code) {
        return switch (defaultString(code, "ANY")) {
            case "WITHIN_1M" -> 1;
            case "WITHIN_3M" -> 2;
            case "WITHIN_6M" -> 3;
            default -> 4;
        };
    }

    private boolean intersects(Collection<?> left, Collection<?> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return false;
        Set<?> rightSet = new HashSet<>(right);
        return left.stream().anyMatch(rightSet::contains);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object value) {
        if (value instanceof List<?> source) {
            return (List<Map<String, Object>>) source;
        }
        return List.of();
    }

    private Set<Long> idSet(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> longValue(row.get(key))).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> longSet(Object value) {
        if (value == null) return Set.of();
        Collection<?> source;
        if (value instanceof Collection<?> collection) source = collection;
        else if (value instanceof String text && text.contains(",")) source = List.of(text.split(","));
        else source = List.of(value);
        return source.stream().map(this::longValue).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> stringSet(Object value) {
        if (value == null) return Set.of();
        Collection<?> source;
        if (value instanceof Collection<?> collection) source = collection;
        else if (value instanceof String text && text.contains(",")) source = List.of(text.split(","));
        else source = List.of(value);
        return source.stream()
                .map(item -> Objects.toString(item, "").trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> doubleMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(Objects.toString(entry.getKey()), doubleValue(entry.getValue()));
        }
        return result;
    }

    private String str(Map<String, Object> row, String key) {
        return Objects.toString(row.get(key), "");
    }

    private String strOrNull(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultString(Object value, String fallback) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? fallback : text;
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private double doubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof BigDecimal decimal) return decimal.doubleValue();
        if (value instanceof Number number) return number.doubleValue();
        return Double.parseDouble(Objects.toString(value, "0"));
    }

    private Double doubleOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Double.parseDouble(text) : null;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record ScoreParts(
            double role,
            double regionDistance,
            double joinTime,
            double collaborationCondition,
            double genre,
            double experience,
            double collaborationStatus,
            double travelRange,
            double durationFit
    ) {
    }

    private record ScorePolicy(Map<String, Double> finalRatio, Map<String, Double> firstFilter, Map<String, Double> internal) {
        double finalRatio(String code) {
            return finalRatio.getOrDefault(code, "first_filter".equals(code) ? 80.0 : 20.0);
        }

        double first(String code) {
            return firstFilter.getOrDefault(code, 0.0);
        }

        double internal(String code) {
            return internal.getOrDefault(code, 0.0);
        }
    }
}
