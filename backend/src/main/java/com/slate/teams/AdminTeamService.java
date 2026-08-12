package com.slate.teams;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.teams.AdminTeamController.AdminTeamCloseRequest;
import com.slate.teams.AdminTeamController.AdminTeamRestoreRequest;
import com.slate.teams.AdminTeamController.AdminTeamUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminTeamService {

    private static final List<String> ACTIVE_TEAM_STATUSES = List.of("RECRUITING", "IN_PROGRESS", "RECRUITMENT_CLOSED", "CLOSING");
    private static final List<String> ADMIN_TEAM_STATUSES = List.of("RECRUITING", "IN_PROGRESS", "RECRUITMENT_CLOSED", "CLOSING", "ENDED", "DELETED");
    private static final Set<String> TEAM_END_TYPES = Set.of("NORMAL", "DISSOLUTION");
    private static final Set<String> RECRUITMENT_STATUSES = Set.of("OPEN", "CLOSED", "DELETED");
    private static final Set<String> SLOT_STATUSES = Set.of("OPEN", "CLOSED", "DELETED");
    private static final Set<String> PLAN_STATUSES = Set.of("TODO", "IN_PROGRESS", "DONE", "HOLD", "CANCELED");

    private final TeamMapper teamMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AdminTeamService(
            TeamMapper teamMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.teamMapper = teamMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> teams(
            Long adminUserId,
            String keyword,
            String status,
            Long regionId,
            Long leaderUserId,
            Integer limit
    ) {
        requireContentModeration(adminUserId);
        return teamMapper.selectAdminTeams(
                textOrNull(keyword),
                normalizeNullable(status, ADMIN_TEAM_STATUSES, "status"),
                regionId,
                leaderUserId,
                safeLimit(limit)
        );
    }

    public Map<String, Object> team(Long adminUserId, Long teamId) {
        requireContentModeration(adminUserId);
        return detail(requireTeam(teamId));
    }

    @Transactional
    public Map<String, Object> updateTeam(Long adminUserId, Long teamId, AdminTeamUpdateRequest request) {
        requireContentModeration(adminUserId);
        String reason = requireReason(request.reason());
        Map<String, Object> before = requireTeam(teamId);
        assertEditable(before);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teamId", teamId);
        row.put("name", normalizeText(request.name(), Objects.toString(before.get("name"), ""), "name"));
        row.put("description", normalizeText(request.description(), Objects.toString(before.get("description"), ""), "description"));
        row.put("status", normalizeValue(request.status(), Objects.toString(before.get("status"), ""), ACTIVE_TEAM_STATUSES, "status"));
        String regionAnyYn = yn(textOrDefault(request.regionAnyYn(), Objects.toString(before.get("regionAnyYn"), "N")));
        row.put("regionAnyYn", regionAnyYn);
        row.put("regionId", "Y".equals(regionAnyYn) ? null : (request.regionId() == null ? before.get("regionId") : request.regionId()));
        row.put("expectedDuration", normalizeText(request.expectedDuration(), Objects.toString(before.get("expectedDuration"), ""), "expectedDuration"));
        int maxMemberCount = request.maxMemberCount() == null ? intValue(before.get("maxMemberCount"), 100) : request.maxMemberCount();
        if (maxMemberCount < intValue(before.get("currentMemberCount"), 0)) {
            throw new SlateException("최대 인원은 현재 팀원 수보다 작게 설정할 수 없습니다.");
        }
        row.put("maxMemberCount", maxMemberCount);
        if (teamMapper.updateAdminTeam(row) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다.");
        }
        Map<String, Object> after = requireTeam(teamId);
        auditLogService.recordAudit(adminUserId, "TEAM_ADMIN_UPDATED", "TEAM", teamId, auditPayload(before, null), auditPayload(after, reason));
        auditLogService.recordOperation(
                "INFO",
                "TEAM_ADMIN_UPDATED",
                "관리자가 팀 정보를 수정했습니다.",
                Map.of("teamId", teamId, "adminUserId", adminUserId, "reason", reason)
        );
        return detail(after);
    }

    @Transactional
    public Map<String, Object> hideTeam(Long adminUserId, Long teamId, String reason) {
        requireContentModeration(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requireTeam(teamId);
        assertEditable(before);
        if ("RECRUITMENT_CLOSED".equals(before.get("status"))) {
            throw new SlateException("이미 모집 중단 상태인 팀입니다.");
        }
        if (teamMapper.updateAdminTeamStatus(teamId, "RECRUITMENT_CLOSED") == 0) {
            throw new SlateException("팀을 숨김 처리하지 못했습니다.");
        }
        Map<String, Object> after = requireTeam(teamId);
        auditLogService.recordAudit(adminUserId, "TEAM_ADMIN_HIDDEN", "TEAM", teamId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "WARN",
                "TEAM_ADMIN_HIDDEN",
                "관리자가 팀을 모집 중단 상태로 전환했습니다.",
                Map.of("teamId", teamId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return detail(after);
    }

    @Transactional
    public Map<String, Object> closeTeam(Long adminUserId, Long teamId, AdminTeamCloseRequest request) {
        requireContentModeration(adminUserId);
        String reason = requireReason(request.reason());
        String endType = normalizeEndType(request.endType());
        Map<String, Object> before = requireTeam(teamId);
        assertEditable(before);
        Map<String, Object> snapshotPayload = closureSnapshotPayload(adminUserId, teamId, endType, reason);
        Map<String, Object> snapshot = insertClosureSnapshot(teamId, endType, adminUserId, snapshotPayload);
        teamMapper.closeRecruitmentsByTeamId(teamId);
        teamMapper.closeSlotsByTeamId(teamId);
        int canceledApplications = teamMapper.cancelPendingApplications(teamId);
        int canceledInvitations = teamMapper.cancelPendingInvitations(teamId);
        if (teamMapper.closeTeam(teamId, endType) == 0) {
            throw new SlateException("팀을 종료하지 못했습니다.");
        }
        Map<String, Object> after = requireTeam(teamId);
        Map<String, Object> auditAfter = auditPayload(after, reason);
        auditAfter.put("closureSnapshotId", longValue(snapshot.get("closureSnapshotId")));
        auditAfter.put("canceledApplications", canceledApplications);
        auditAfter.put("canceledInvitations", canceledInvitations);
        auditLogService.recordAudit(adminUserId, "TEAM_ADMIN_CLOSED_" + endType, "TEAM", teamId, auditPayload(before, null), auditAfter);
        auditLogService.recordAudit(adminUserId, "TEAM_CLOSURE_SNAPSHOT_CREATED", "TEAM_CLOSURE_SNAPSHOT", longValue(snapshot.get("closureSnapshotId")), null, snapshotPayload);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_ADMIN_CLOSED",
                "관리자가 팀을 종료하고 대기 요청을 정리했습니다.",
                Map.of("teamId", teamId, "adminUserId", adminUserId, "endType", endType, "reason", reason)
        );
        return detail(after);
    }

    @Transactional
    public Map<String, Object> deleteTeam(Long adminUserId, Long teamId, String reason) {
        requireContentModeration(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requireTeam(teamId);
        if ("DELETED".equals(before.get("status"))) {
            throw new SlateException("이미 삭제 상태인 팀입니다.");
        }
        Map<String, Object> snapshotPayload = closureSnapshotPayload(adminUserId, teamId, "DISSOLUTION", cleanReason);
        Map<String, Object> snapshot = insertClosureSnapshot(teamId, "DISSOLUTION", adminUserId, snapshotPayload);
        teamMapper.softDeleteSlotsByTeamId(teamId);
        teamMapper.softDeleteRecruitmentsByTeamId(teamId);
        int canceledApplications = teamMapper.cancelPendingApplications(teamId);
        int canceledInvitations = teamMapper.cancelPendingInvitations(teamId);
        if (teamMapper.softDeleteAdminTeam(teamId) == 0) {
            throw new SlateException("팀을 삭제 상태로 전환하지 못했습니다.");
        }
        Map<String, Object> after = requireTeam(teamId);
        Map<String, Object> auditAfter = auditPayload(after, cleanReason);
        auditAfter.put("closureSnapshotId", longValue(snapshot.get("closureSnapshotId")));
        auditAfter.put("canceledApplications", canceledApplications);
        auditAfter.put("canceledInvitations", canceledInvitations);
        auditLogService.recordAudit(adminUserId, "TEAM_ADMIN_DELETED", "TEAM", teamId, auditPayload(before, null), auditAfter);
        auditLogService.recordAudit(adminUserId, "TEAM_CLOSURE_SNAPSHOT_CREATED", "TEAM_CLOSURE_SNAPSHOT", longValue(snapshot.get("closureSnapshotId")), null, snapshotPayload);
        auditLogService.recordOperation(
                "WARN",
                "TEAM_ADMIN_DELETED",
                "관리자가 팀을 삭제 상태로 전환했습니다.",
                Map.of("teamId", teamId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return detail(after);
    }

    @Transactional
    public Map<String, Object> restoreTeam(Long adminUserId, Long teamId, AdminTeamRestoreRequest request) {
        requireContentModeration(adminUserId);
        String reason = requireReason(request.reason());
        Map<String, Object> before = requireTeam(teamId);
        String beforeStatus = Objects.toString(before.get("status"), "");
        if (!List.of("ENDED", "DELETED", "RECRUITMENT_CLOSED").contains(beforeStatus)) {
            throw new SlateException("종료, 삭제 또는 모집 중단 상태의 팀만 복구할 수 있습니다.");
        }
        Map<String, Object> snapshot = resolveClosureSnapshot(teamId, request.closureSnapshotId());
        Map<String, Object> snapshotPayload = parseClosureSnapshot(snapshot);
        RestoreResult restoreResult = restoreFromSnapshot(teamId, snapshotPayload, request);
        int updated;
        if ("ENDED".equals(beforeStatus)) {
            updated = teamMapper.reopenTeam(teamId, restoreResult.teamStatus());
        } else if ("DELETED".equals(beforeStatus)) {
            updated = teamMapper.restoreAdminTeam(teamId, restoreResult.teamStatus());
        } else {
            updated = teamMapper.updateAdminTeamStatus(teamId, restoreResult.teamStatus());
        }
        if (updated == 0) {
            throw new SlateException("팀을 복구하지 못했습니다.");
        }
        Map<String, Object> after = requireTeam(teamId);
        Map<String, Object> auditAfter = auditPayload(after, reason);
        auditAfter.put("closureSnapshotId", snapshot == null ? null : longValue(snapshot.get("closureSnapshotId")));
        auditAfter.put("restoreSnapshotYn", restoreResult.restoreSnapshotYn());
        auditAfter.put("restoredRecruitments", restoreResult.restoredRecruitments());
        auditAfter.put("restoredSlots", restoreResult.restoredSlots());
        auditAfter.put("restoredPlans", restoreResult.restoredPlans());
        auditLogService.recordAudit(adminUserId, "TEAM_ADMIN_RESTORED", "TEAM", teamId, auditPayload(before, null), auditAfter);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_ADMIN_RESTORED",
                "관리자가 팀을 복구했습니다.",
                Map.of("teamId", teamId, "adminUserId", adminUserId, "reason", reason, "teamStatus", restoreResult.teamStatus())
        );
        return detail(after);
    }

    private void requireContentModeration(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
    }

    private Map<String, Object> requireTeam(Long teamId) {
        Map<String, Object> team = teamMapper.selectAdminTeamById(teamId);
        if (team == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다.");
        }
        return team;
    }

    private Map<String, Object> detail(Map<String, Object> team) {
        Long teamId = longValue(team.get("teamId"));
        Map<String, Object> result = new LinkedHashMap<>(team);
        result.put("genres", teamMapper.selectTeamGenres(teamId));
        result.put("members", teamMapper.selectTeamMembers(teamId));
        result.put("recruitments", recruitments(teamId));
        result.put("closureSnapshots", teamMapper.selectClosureSnapshotsByTeamId(teamId));
        result.put("plans", teamMapper.selectPlanItemsByTeamId(teamId));
        return result;
    }

    private List<Map<String, Object>> recruitments(Long teamId) {
        return teamMapper.selectRecruitmentsByTeamId(teamId).stream()
                .map(recruitment -> {
                    Map<String, Object> row = new LinkedHashMap<>(recruitment);
                    row.put("slots", teamMapper.selectSlotsByRecruitmentId(longValue(recruitment.get("recruitmentId"))));
                    return row;
                })
                .toList();
    }

    private void assertEditable(Map<String, Object> team) {
        if ("ENDED".equals(team.get("status"))) {
            throw new SlateException("종료된 팀은 복구 후 수정할 수 있습니다.");
        }
        if ("DELETED".equals(team.get("status"))) {
            throw new SlateException("삭제 상태의 팀은 복구 후 수정할 수 있습니다.");
        }
    }

    private Map<String, Object> insertClosureSnapshot(Long teamId, String endType, Long adminUserId, Map<String, Object> snapshotPayload) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teamId", teamId);
        snapshot.put("endType", endType);
        snapshot.put("snapshotJson", toJson(snapshotPayload));
        snapshot.put("createdBy", adminUserId);
        teamMapper.insertClosureSnapshot(snapshot);
        return snapshot;
    }

    private Map<String, Object> closureSnapshotPayload(Long adminUserId, Long teamId, String endType, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> team = detail(requireTeam(teamId));
        payload.put("team", team);
        payload.put("endType", endType);
        payload.put("reason", reason);
        payload.put("createdBy", adminUserId);
        payload.put("applications", teamMapper.selectApplicationsByTeamId(teamId));
        payload.put("invitations", teamMapper.selectInvitationsByTeamId(teamId));
        payload.put("summary", Map.of(
                "memberCount", intValue(team.get("currentMemberCount"), 0),
                "recruitmentCount", ((List<?>) team.get("recruitments")).size(),
                "planCount", ((List<?>) team.get("plans")).size()
        ));
        return payload;
    }

    private Map<String, Object> resolveClosureSnapshot(Long teamId, Long closureSnapshotId) {
        Map<String, Object> snapshot = closureSnapshotId == null
                ? teamMapper.selectLatestClosureSnapshotByTeamId(teamId)
                : teamMapper.selectClosureSnapshotById(closureSnapshotId);
        if (closureSnapshotId != null && snapshot == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "종료 스냅샷을 찾을 수 없습니다.");
        }
        if (snapshot != null && !Objects.equals(longValue(snapshot.get("teamId")), teamId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "다른 팀의 종료 스냅샷은 사용할 수 없습니다.");
        }
        return snapshot;
    }

    private Map<String, Object> parseClosureSnapshot(Map<String, Object> snapshot) {
        String snapshotJson = snapshot == null ? null : Objects.toString(snapshot.get("snapshotJson"), null);
        if (!StringUtils.hasText(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "팀 종료 스냅샷 JSON을 읽지 못했습니다.");
        }
    }

    private RestoreResult restoreFromSnapshot(Long teamId, Map<String, Object> snapshotPayload, AdminTeamRestoreRequest request) {
        boolean restoreSnapshot = "Y".equals(yn(textOrDefault(request.restoreSnapshotYn(), "Y")));
        String teamStatus = normalizeRestoreStatus(request.status(), restoredTeamStatus(snapshotPayload));
        int restoredRecruitments = 0;
        int restoredSlots = 0;
        int restoredPlans = 0;
        if (restoreSnapshot && !snapshotPayload.isEmpty()) {
            for (Map<String, Object> recruitment : mapList(snapshotPayload.get("recruitments"))) {
                Long recruitmentId = longValue(recruitment.get("recruitmentId"));
                if (recruitmentId == null) {
                    continue;
                }
                restoredRecruitments += teamMapper.restoreRecruitmentStatusAsAdmin(teamId, recruitmentId, normalizeRecruitmentStatus(recruitment.get("status")));
                for (Map<String, Object> slot : mapList(recruitment.get("slots"))) {
                    Long slotId = longValue(slot.get("slotId"));
                    if (slotId != null) {
                        restoredSlots += teamMapper.restoreSlotStatusAsAdmin(teamId, slotId, normalizeSlotStatus(slot.get("status")));
                    }
                }
            }
            for (Map<String, Object> plan : mapList(snapshotPayload.get("plans"))) {
                Long planItemId = longValue(plan.get("planItemId"));
                if (planItemId != null) {
                    restoredPlans += teamMapper.restorePlanItemStatus(teamId, planItemId, normalizePlanStatus(plan.get("status")));
                }
            }
        }
        return new RestoreResult(teamStatus, restoreSnapshot ? "Y" : "N", restoredRecruitments, restoredSlots, restoredPlans);
    }

    private String restoredTeamStatus(Map<String, Object> snapshotPayload) {
        Map<String, Object> team = mapValue(snapshotPayload.get("team"));
        return normalizeRestoreStatus(null, Objects.toString(team.get("status"), "RECRUITING"));
    }

    private String normalizeRestoreStatus(String value, String fallback) {
        String safeFallback = StringUtils.hasText(fallback) && ACTIVE_TEAM_STATUSES.contains(fallback)
                ? fallback
                : "RECRUITING";
        return normalizeValue(value, safeFallback, ACTIVE_TEAM_STATUSES, "status");
    }

    private String normalizeRecruitmentStatus(Object value) {
        String status = Objects.toString(value, "CLOSED").trim().toUpperCase(Locale.ROOT);
        return RECRUITMENT_STATUSES.contains(status) ? status : "CLOSED";
    }

    private String normalizeSlotStatus(Object value) {
        String status = Objects.toString(value, "CLOSED").trim().toUpperCase(Locale.ROOT);
        return SLOT_STATUSES.contains(status) ? status : "CLOSED";
    }

    private String normalizePlanStatus(Object value) {
        String status = Objects.toString(value, "TODO").trim().toUpperCase(Locale.ROOT);
        return PLAN_STATUSES.contains(status) ? status : "TODO";
    }

    private String normalizeEndType(String value) {
        String endType = textOrDefault(value, "NORMAL").toUpperCase(Locale.ROOT);
        if (!TEAM_END_TYPES.contains(endType)) {
            throw new SlateException("endType은 NORMAL 또는 DISSOLUTION만 가능합니다.");
        }
        return endType;
    }

    private String normalizeText(String value, String fallback, String fieldName) {
        if (value != null && !StringUtils.hasText(value)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        String result = value != null ? value.trim() : fallback;
        if (!StringUtils.hasText(result)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        return result;
    }

    private String normalizeValue(String value, String fallback, List<String> allowed, String fieldName) {
        if (value != null && !StringUtils.hasText(value)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        String normalized = value != null ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!allowed.contains(normalized)) {
            throw new SlateException(fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value, List<String> allowed, String fieldName) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return normalizeValue(value, "", allowed, fieldName);
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new SlateException("관리자 처리 사유는 필수입니다.");
        }
        return reason.trim();
    }

    private String yn(String value) {
        String text = textOrDefault(value, "N");
        return "Y".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text) ? "Y" : "N";
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 50 : limit, 100));
    }

    private int intValue(Object value, int fallback) {
        Long converted = longValue(value);
        return converted == null ? fallback : converted.intValue();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> rows)) {
            return List.of();
        }
        return rows.stream()
                .map(this::mapValue)
                .filter(row -> !row.isEmpty())
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(jsonSafe(value));
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "팀 종료 스냅샷 JSON 변환 중 오류가 발생했습니다.");
        }
    }

    private Object jsonSafe(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), jsonSafe(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            iterable.forEach(item -> result.add(jsonSafe(item)));
            return result;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                result.add(jsonSafe(Array.get(value, index)));
            }
            return result;
        }
        return String.valueOf(value);
    }

    private Map<String, Object> auditPayload(Map<String, Object> row, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "teamId", "name", "description", "status", "endType", "leaderUserId", "leaderNickname",
                "regionId", "regionDisplayName", "regionAnyYn", "expectedDuration",
                "maxMemberCount", "currentMemberCount", "activeRecruitmentCount",
                "pendingApplicationCount", "createdAt", "updatedAt"
        )) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        if (reason != null) {
            result.put("reason", reason);
        }
        return result;
    }

    private record RestoreResult(
            String teamStatus,
            String restoreSnapshotYn,
            int restoredRecruitments,
            int restoredSlots,
            int restoredPlans
    ) {
    }
}
