package com.slate.teams;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import com.slate.common.SlateException;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import com.slate.teams.TeamController.InvitationDecision;
import com.slate.teams.TeamController.MemberUpdateRequest;
import com.slate.teams.TeamController.PlanItemRequest;
import com.slate.teams.TeamController.PlanStatusRequest;
import com.slate.teams.TeamController.RecruitmentRequest;
import com.slate.teams.TeamController.RequestDecision;
import com.slate.teams.TeamController.SlotRequest;
import com.slate.teams.TeamController.TeamCloseRequest;
import com.slate.teams.TeamController.TeamLeaderTransferRequest;
import com.slate.teams.TeamController.TeamRequest;
import com.slate.teams.TeamController.TeamReopenRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TeamService {

    private static final Set<String> TEAM_ROLES = Set.of("LEADER", "SUB_LEADER", "MEMBER");
    private static final Set<String> PLAN_STATUSES = Set.of("TODO", "IN_PROGRESS", "DONE", "HOLD", "CANCELED");
    private static final Set<String> TEAM_STATUSES = Set.of("RECRUITING", "IN_PROGRESS", "RECRUITMENT_CLOSED", "CLOSING");
    private static final Set<String> TEAM_END_TYPES = Set.of("NORMAL", "DISSOLUTION");
    private static final Set<String> RECRUITMENT_STATUSES = Set.of("OPEN", "CLOSED");
    private static final Set<String> SLOT_STATUSES = Set.of("OPEN", "CLOSED");

    private final TeamMapper teamMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public TeamService(
            TeamMapper teamMapper,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.teamMapper = teamMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> myTeams(Long userId) {
        return teamMapper.selectTeamsByUserId(userId).stream().map(this::enrich).toList();
    }

    public Map<String, Object> team(Long teamId) {
        Map<String, Object> team = teamMapper.selectTeamById(teamId);
        if (team == null) {
            throw new SlateException("팀을 찾을 수 없습니다.");
        }
        return enrich(team);
    }

    @Transactional
    public Map<String, Object> createTeam(Long userId, TeamRequest request) {
        if (teamMapper.countActiveTeamsByUserId(userId) >= 3) {
            throw new SlateException("참여 중인 팀은 최대 3개까지 가능합니다.");
        }
        Map<String, Object> team = teamMap(userId, null, request);
        teamMapper.insertTeam(team);
        Long teamId = ((Number) team.get("teamId")).longValue();
        replaceTeamGenres(teamId, request.genreIds());
        teamMapper.insertTeamMember(teamId, userId, "LEADER");
        Map<String, Object> created = team(teamId);
        auditLogService.recordAudit(userId, "TEAM_CREATED", "TEAM", teamId, null, auditPayload(created));
        return created;
    }

    @Transactional
    public Map<String, Object> updateTeam(Long userId, Long teamId, TeamRequest request) {
        assertLeader(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> before = team(teamId);
        Map<String, Object> team = teamMap(userId, teamId, request);
        if (teamMapper.updateTeam(team) == 0) {
            throw new SlateException("수정할 팀을 찾을 수 없습니다.");
        }
        replaceTeamGenres(teamId, request.genreIds());
        Map<String, Object> updated = team(teamId);
        auditLogService.recordAudit(userId, "TEAM_UPDATED", "TEAM", teamId, auditPayload(before), auditPayload(updated));
        return updated;
    }

    @Transactional
    public Map<String, Object> deleteTeam(Long userId, Long teamId) {
        assertLeader(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> before = team(teamId);
        Map<String, Object> snapshotPayload = closureSnapshotPayload(userId, teamId, "DISSOLUTION", "팀장이 팀을 삭제했습니다.");
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teamId", teamId);
        snapshot.put("endType", "DISSOLUTION");
        snapshot.put("snapshotJson", toJson(snapshotPayload));
        snapshot.put("createdBy", userId);
        teamMapper.insertClosureSnapshot(snapshot);
        teamMapper.softDeleteSlotsByTeamId(teamId);
        teamMapper.softDeleteRecruitmentsByTeamId(teamId);
        int canceledApplications = teamMapper.cancelPendingApplications(teamId);
        int canceledInvitations = teamMapper.cancelPendingInvitations(teamId);
        if (teamMapper.softDeleteTeam(teamId) == 0) {
            throw new SlateException("삭제할 수 있는 팀을 찾을 수 없습니다.");
        }
        notifyTeamClosed(userId, before, "DISSOLUTION", "팀장이 팀을 삭제했습니다.");
        Long snapshotId = longValue(snapshot.get("closureSnapshotId"));
        Map<String, Object> deleted = new LinkedHashMap<>(before);
        deleted.put("status", "DELETED");
        deleted.put("endType", "DISSOLUTION");
        deleted.put("closureSnapshotId", snapshotId);
        deleted.put("canceledApplications", canceledApplications);
        deleted.put("canceledInvitations", canceledInvitations);
        auditLogService.recordAudit(userId, "TEAM_DELETED", "TEAM", teamId, auditPayload(before), auditPayload(deleted));
        auditLogService.recordAudit(userId, "TEAM_CLOSURE_SNAPSHOT_CREATED", "TEAM_CLOSURE_SNAPSHOT", snapshotId, null, snapshotPayload);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_DELETED",
                "팀이 소프트 삭제되고 모집과 대기 요청이 정리되었습니다.",
                Map.of("teamId", teamId, "closureSnapshotId", snapshotId, "canceledApplications", canceledApplications, "canceledInvitations", canceledInvitations)
        );
        return deleted;
    }

    @Transactional
    public List<Map<String, Object>> recruitments(Long teamId) {
        teamMapper.closeExpiredRecruitmentsByTeamId(teamId);
        return teamMapper.selectRecruitmentsByTeamId(teamId).stream()
                .map(recruitment -> {
                    Map<String, Object> row = new LinkedHashMap<>(recruitment);
                    row.put("slots", teamMapper.selectSlotsByRecruitmentId(((Number) recruitment.get("recruitmentId")).longValue()));
                    return row;
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> createRecruitment(Long userId, Long teamId, RecruitmentRequest request) {
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> recruitment = recruitmentMap(null, teamId, userId, request);
        teamMapper.insertRecruitment(recruitment);
        Map<String, Object> created = teamMapper.selectRecruitmentById(((Number) recruitment.get("recruitmentId")).longValue());
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_CREATED", "TEAM_RECRUITMENT", longValue(created.get("recruitmentId")), null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> updateRecruitment(Long userId, Long recruitmentId, RecruitmentRequest request) {
        Map<String, Object> existing = teamMapper.selectRecruitmentById(recruitmentId);
        if (existing == null || "DELETED".equals(existing.get("status"))) {
            throw new SlateException("모집 공고를 찾을 수 없습니다.");
        }
        Long teamId = ((Number) existing.get("teamId")).longValue();
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> recruitment = recruitmentMap(recruitmentId, teamId, userId, request);
        if (teamMapper.updateRecruitment(recruitment) == 0) {
            throw new SlateException("모집 공고를 수정하지 못했습니다.");
        }
        Map<String, Object> updated = teamMapper.selectRecruitmentById(recruitmentId);
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_UPDATED", "TEAM_RECRUITMENT", recruitmentId, existing, updated);
        return updated;
    }

    @Transactional
    public Map<String, Object> deleteRecruitment(Long userId, Long recruitmentId) {
        Map<String, Object> existing = teamMapper.selectRecruitmentById(recruitmentId);
        if (existing == null || "DELETED".equals(existing.get("status"))) {
            throw new SlateException("모집 공고를 찾을 수 없습니다.");
        }
        Long teamId = longValue(existing.get("teamId"));
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        int canceledApplications = teamMapper.cancelPendingApplicationsByRecruitment(recruitmentId);
        int canceledInvitations = teamMapper.cancelPendingInvitationsByRecruitment(recruitmentId);
        teamMapper.softDeleteSlotsByRecruitment(recruitmentId);
        if (teamMapper.softDeleteRecruitment(recruitmentId) == 0) {
            throw new SlateException("모집 공고를 삭제하지 못했습니다.");
        }
        Map<String, Object> deleted = new LinkedHashMap<>(existing);
        deleted.put("status", "DELETED");
        deleted.put("canceledApplications", canceledApplications);
        deleted.put("canceledInvitations", canceledInvitations);
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_DELETED", "TEAM_RECRUITMENT", recruitmentId, existing, deleted);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_RECRUITMENT_DELETED",
                "모집 공고가 소프트 삭제되고 대기 요청이 취소되었습니다.",
                Map.of("teamId", teamId, "recruitmentId", recruitmentId, "canceledApplications", canceledApplications, "canceledInvitations", canceledInvitations)
        );
        return deleted;
    }

    @Transactional
    public Map<String, Object> createSlot(Long userId, Long recruitmentId, SlotRequest request) {
        Map<String, Object> recruitment = teamMapper.selectRecruitmentById(recruitmentId);
        if (recruitment == null || "DELETED".equals(recruitment.get("status"))) {
            throw new SlateException("모집 공고를 찾을 수 없습니다.");
        }
        assertManager(userId, ((Number) recruitment.get("teamId")).longValue());
        assertTeamOpen(((Number) recruitment.get("teamId")).longValue());
        Map<String, Object> slot = slotMap(null, recruitmentId, request);
        teamMapper.insertRecruitmentSlot(slot);
        Map<String, Object> created = teamMapper.selectSlotById(((Number) slot.get("slotId")).longValue());
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_SLOT_CREATED", "TEAM_RECRUITMENT_SLOT", longValue(created.get("slotId")), null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> updateSlot(Long userId, Long slotId, SlotRequest request) {
        Map<String, Object> existing = teamMapper.selectSlotById(slotId);
        if (existing == null || "DELETED".equals(existing.get("status"))) {
            throw new SlateException("모집 슬롯을 찾을 수 없습니다.");
        }
        assertManager(userId, ((Number) existing.get("teamId")).longValue());
        assertTeamOpen(((Number) existing.get("teamId")).longValue());
        Map<String, Object> slot = slotMap(slotId, ((Number) existing.get("recruitmentId")).longValue(), request);
        if (teamMapper.updateRecruitmentSlot(slot) == 0) {
            throw new SlateException("모집 슬롯을 수정하지 못했습니다.");
        }
        Map<String, Object> updated = teamMapper.selectSlotById(slotId);
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_SLOT_UPDATED", "TEAM_RECRUITMENT_SLOT", slotId, existing, updated);
        return updated;
    }

    @Transactional
    public Map<String, Object> deleteSlot(Long userId, Long slotId) {
        Map<String, Object> existing = teamMapper.selectSlotById(slotId);
        if (existing == null || "DELETED".equals(existing.get("status"))) {
            throw new SlateException("모집 슬롯을 찾을 수 없습니다.");
        }
        Long teamId = longValue(existing.get("teamId"));
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        int canceledApplications = teamMapper.cancelPendingApplicationsBySlot(slotId);
        int canceledInvitations = teamMapper.cancelPendingInvitationsBySlot(slotId);
        if (teamMapper.softDeleteSlot(slotId) == 0) {
            throw new SlateException("모집 슬롯을 삭제하지 못했습니다.");
        }
        Map<String, Object> deleted = new LinkedHashMap<>(existing);
        deleted.put("status", "DELETED");
        deleted.put("canceledApplications", canceledApplications);
        deleted.put("canceledInvitations", canceledInvitations);
        auditLogService.recordAudit(userId, "TEAM_RECRUITMENT_SLOT_DELETED", "TEAM_RECRUITMENT_SLOT", slotId, existing, deleted);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_RECRUITMENT_SLOT_DELETED",
                "모집 슬롯이 소프트 삭제되고 대기 요청이 취소되었습니다.",
                Map.of("teamId", teamId, "slotId", slotId, "canceledApplications", canceledApplications, "canceledInvitations", canceledInvitations)
        );
        return deleted;
    }

    public List<Map<String, Object>> applications(Long userId, Long teamId) {
        assertManager(userId, teamId);
        return teamMapper.selectApplicationsByTeamId(teamId);
    }

    @Transactional
    public Map<String, Object> decideApplication(Long managerUserId, Long applicationId, RequestDecision request) {
        Map<String, Object> before = requireApplication(applicationId);
        Long teamId = longValue(before.get("teamId"));
        assertManager(managerUserId, teamId);
        assertTeamOpen(teamId);
        String decision = decision(request.decision());
        Long applicantUserId = longValue(before.get("applicantUserId"));
        if ("ACCEPTED".equals(decision)) {
            assertCanAddMember(teamId, applicantUserId);
        }
        if (teamMapper.updateApplicationDecision(applicationId, decision, "REJECTED".equals(decision) ? textOrDefault(request.reason(), "팀 구성상 이번에는 함께하기 어렵습니다.") : null, managerUserId) == 0) {
            throw new SlateException("이미 처리된 지원입니다.");
        }
        if ("ACCEPTED".equals(decision)) {
            activateMember(teamId, applicantUserId, before);
        }
        Map<String, Object> after = requireApplication(applicationId);
        notificationService.send(
                applicantUserId,
                managerUserId,
                "TEAM",
                "ACCEPTED".equals(decision) ? "팀 지원이 수락되었습니다." : "팀 지원이 거절되었습니다.",
                "ACCEPTED".equals(decision)
                        ? textOrDefault((String) before.get("teamName"), "지원한 팀") + "에 합류했습니다."
                        : "지원 결과가 거절로 처리되었습니다. 사유: " + textOrDefault(request.reason(), "팀 구성상 이번에는 함께하기 어렵습니다."),
                "TEAM",
                teamId
        );
        auditLogService.recordAudit(managerUserId, "TEAM_APPLICATION_" + decision, "TEAM_APPLICATION", applicationId, before, after);
        return after;
    }

    public List<Map<String, Object>> teamInvitations(Long userId, Long teamId) {
        assertManager(userId, teamId);
        return teamMapper.selectInvitationsByTeamId(teamId);
    }

    public List<Map<String, Object>> myInvitations(Long userId) {
        return teamMapper.selectInvitationsByTargetUserId(userId);
    }

    @Transactional
    public Map<String, Object> decideInvitation(Long userId, Long invitationId, InvitationDecision request) {
        Map<String, Object> before = requireInvitation(invitationId);
        Long targetUserId = longValue(before.get("targetUserId"));
        if (!targetUserId.equals(userId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "초대 대상자만 응답할 수 있습니다.");
        }
        String decision = decision(request.decision());
        Long teamId = longValue(before.get("teamId"));
        assertTeamOpen(teamId);
        if ("ACCEPTED".equals(decision)) {
            assertCanAddMember(teamId, userId);
        }
        if (teamMapper.updateInvitationDecision(invitationId, decision) == 0) {
            throw new SlateException("이미 처리된 초대입니다.");
        }
        if ("ACCEPTED".equals(decision)) {
            activateMember(teamId, userId, before);
        }
        Map<String, Object> after = requireInvitation(invitationId);
        notificationService.sendToTeamManagers(
                teamId,
                userId,
                "ACCEPTED".equals(decision) ? "팀 초대가 수락되었습니다." : "팀 초대가 거절되었습니다.",
                textOrDefault((String) before.get("targetNickname"), "초대 대상자") + "님이 초대에 응답했습니다.",
                "TEAM",
                teamId
        );
        auditLogService.recordAudit(userId, "TEAM_INVITATION_" + decision, "TEAM_INVITATION", invitationId, before, after);
        return after;
    }

    @Transactional
    public Map<String, Object> updateMember(Long managerUserId, Long teamId, Long memberUserId, MemberUpdateRequest request) {
        assertLeader(managerUserId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> before = requireTeamMember(teamId, memberUserId);
        if ("LEADER".equals(before.get("teamRole"))) {
            throw new SlateException("팀장 권한은 이 화면에서 변경할 수 없습니다.");
        }
        String status = textOrDefault(request.status(), "ACTIVE").toUpperCase(Locale.ROOT);
        if ("KICKED".equals(status)) {
            teamMapper.updateTeamMemberStatus(teamId, memberUserId, "KICKED");
            teamMapper.recountTeamMemberCount(teamId);
            notificationService.send(memberUserId, managerUserId, "TEAM", "팀에서 제외되었습니다.", team(teamId).get("name") + "에서 제외되었습니다.", "TEAM", teamId);
        } else {
            String role = normalizeTeamRole(request.teamRole());
            if ("LEADER".equals(role)) {
                throw new SlateException("팀장은 한 명만 유지합니다.");
            }
            teamMapper.updateTeamMemberRole(teamId, memberUserId, role);
            notificationService.send(memberUserId, managerUserId, "TEAM", "팀 권한이 변경되었습니다.", "팀 내 권한이 " + role + "(으)로 변경되었습니다.", "TEAM", teamId);
        }
        Map<String, Object> after = requireTeamMember(teamId, memberUserId);
        auditLogService.recordAudit(managerUserId, "TEAM_MEMBER_UPDATED", "TEAM_MEMBER", longValue(after.get("teamMemberId")), before, after);
        return after;
    }

    @Transactional
    public Map<String, Object> leaveTeam(Long userId, Long teamId) {
        assertTeamOpen(teamId);
        Map<String, Object> before = requireTeamMember(teamId, userId);
        if ("LEADER".equals(before.get("teamRole"))) {
            throw new SlateException("팀장은 팀을 나가기 전에 팀 종료 또는 권한 이전이 필요합니다.");
        }
        teamMapper.updateTeamMemberStatus(teamId, userId, "LEFT");
        teamMapper.recountTeamMemberCount(teamId);
        Map<String, Object> after = requireTeamMember(teamId, userId);
        auditLogService.recordAudit(userId, "TEAM_MEMBER_LEFT", "TEAM_MEMBER", longValue(after.get("teamMemberId")), before, after);
        notificationService.sendToTeamManagers(teamId, userId, "팀원이 나갔습니다.", textOrDefault((String) before.get("nickname"), "팀원") + "님이 팀을 나갔습니다.", "TEAM", teamId);
        return after;
    }

    @Transactional
    public Map<String, Object> transferLeader(Long userId, Long teamId, TeamLeaderTransferRequest request) {
        assertLeader(userId, teamId);
        assertTeamOpen(teamId);
        Long newLeaderUserId = request.newLeaderUserId();
        if (Objects.equals(userId, newLeaderUserId)) {
            throw new SlateException("현재 팀장에게 다시 팀장 권한을 이전할 수 없습니다.");
        }
        Map<String, Object> beforeTeam = team(teamId);
        Map<String, Object> beforeOldLeader = requireTeamMember(teamId, userId);
        Map<String, Object> beforeNewLeader = requireTeamMember(teamId, newLeaderUserId);
        if (!"ACTIVE".equals(beforeNewLeader.get("status"))) {
            throw new SlateException("활성 팀원에게만 팀장 권한을 이전할 수 있습니다.");
        }
        if ("LEADER".equals(beforeNewLeader.get("teamRole"))) {
            throw new SlateException("이미 팀장인 사용자입니다.");
        }
        if (teamMapper.updateTeamMemberRole(teamId, userId, "SUB_LEADER") == 0
                || teamMapper.updateTeamMemberRole(teamId, newLeaderUserId, "LEADER") == 0
                || teamMapper.updateTeamLeader(teamId, newLeaderUserId) == 0) {
            throw new SlateException("팀장 권한을 이전하지 못했습니다.");
        }
        Map<String, Object> afterTeam = team(teamId);
        String reason = textOrNull(request.reason());
        String body = textOrDefault((String) beforeTeam.get("name"), "팀") + " 팀장이 "
                + textOrDefault((String) beforeNewLeader.get("nickname"), "새 팀장") + "님으로 변경되었습니다."
                + (reason == null ? "" : " 사유: " + reason);
        notifyActiveTeamMembers(userId, teamId, "팀장 권한이 이전되었습니다.", body);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("team", auditPayload(beforeTeam));
        before.put("oldLeader", auditPayload(beforeOldLeader));
        before.put("newLeader", auditPayload(beforeNewLeader));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("team", auditPayload(afterTeam));
        after.put("oldLeaderUserId", userId);
        after.put("oldLeaderRole", "SUB_LEADER");
        after.put("newLeaderUserId", newLeaderUserId);
        after.put("newLeaderRole", "LEADER");
        after.put("reason", reason);
        auditLogService.recordAudit(userId, "TEAM_LEADER_TRANSFERRED", "TEAM", teamId, before, after);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_LEADER_TRANSFERRED",
                "팀장 권한이 활성 팀원에게 이전되었습니다.",
                Map.of("teamId", teamId, "oldLeaderUserId", userId, "newLeaderUserId", newLeaderUserId)
        );
        return afterTeam;
    }

    @Transactional
    public Map<String, Object> closeTeam(Long userId, Long teamId, TeamCloseRequest request) {
        assertLeader(userId, teamId);
        assertTeamOpen(teamId);
        String endType = normalizeEndType(request.endType());
        Map<String, Object> before = team(teamId);
        Map<String, Object> snapshotPayload = closureSnapshotPayload(userId, teamId, endType, request.reason());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teamId", teamId);
        snapshot.put("endType", endType);
        snapshot.put("snapshotJson", toJson(snapshotPayload));
        snapshot.put("createdBy", userId);
        teamMapper.insertClosureSnapshot(snapshot);
        teamMapper.closeRecruitmentsByTeamId(teamId);
        teamMapper.closeSlotsByTeamId(teamId);
        int canceledApplications = teamMapper.cancelPendingApplications(teamId);
        int canceledInvitations = teamMapper.cancelPendingInvitations(teamId);
        if (teamMapper.closeTeam(teamId, endType) == 0) {
            throw new SlateException("이미 종료된 팀입니다.");
        }
        Map<String, Object> after = team(teamId);
        Long snapshotId = longValue(snapshot.get("closureSnapshotId"));
        notifyTeamClosed(userId, before, endType, request.reason());
        auditLogService.recordAudit(
                userId,
                "TEAM_CLOSED_" + endType,
                "TEAM",
                teamId,
                auditPayload(before),
                Map.of(
                        "teamId", teamId,
                        "status", after.get("status"),
                        "endType", endType,
                        "closureSnapshotId", snapshotId,
                        "canceledApplications", canceledApplications,
                        "canceledInvitations", canceledInvitations
                )
        );
        auditLogService.recordAudit(userId, "TEAM_CLOSURE_SNAPSHOT_CREATED", "TEAM_CLOSURE_SNAPSHOT", snapshotId, null, snapshotPayload);
        auditLogService.recordOperation(
                "INFO",
                "TEAM_CLOSED",
                "팀 종료 스냅샷이 저장되고 팀 상태가 종료로 변경되었습니다.",
                Map.of("teamId", teamId, "endType", endType, "closureSnapshotId", snapshotId)
        );
        return team(teamId);
    }

    public List<Map<String, Object>> closureSnapshots(Long userId, Long teamId) {
        assertActiveMember(userId, teamId);
        return teamMapper.selectClosureSnapshotsByTeamId(teamId);
    }

    @Transactional
    public Map<String, Object> reopenTeam(Long userId, Long teamId, TeamReopenRequest request) {
        assertLeader(userId, teamId);
        Map<String, Object> before = team(teamId);
        if (!"ENDED".equals(before.get("status"))) {
            throw new SlateException("종료된 팀만 재개할 수 있습니다.");
        }
        Map<String, Object> snapshot = resolveClosureSnapshot(teamId, request.closureSnapshotId());
        Map<String, Object> snapshotPayload = parseClosureSnapshot(snapshot);
        boolean restoreSnapshot = "Y".equals(yn(textOrDefault(request.restoreSnapshotYn(), "Y")));
        RestoreResult restoreResult = restoreFromSnapshot(teamId, snapshotPayload, restoreSnapshot);
        if (teamMapper.reopenTeam(teamId, restoreResult.teamStatus()) == 0) {
            throw new SlateException("팀을 재개하지 못했습니다.");
        }
        Map<String, Object> after = team(teamId);
        String reason = textOrNull(request.reason());
        String body = textOrDefault((String) after.get("name"), "팀") + " 상태가 " + restoreResult.teamStatus() + "(으)로 재개되었습니다."
                + (reason == null ? "" : " 사유: " + reason);
        notifyActiveTeamMembers(userId, teamId, "팀이 재개되었습니다.", body);

        Map<String, Object> auditAfter = new LinkedHashMap<>(auditPayload(after));
        auditAfter.put("closureSnapshotId", snapshot == null ? null : longValue(snapshot.get("closureSnapshotId")));
        auditAfter.put("restoreSnapshotYn", restoreSnapshot ? "Y" : "N");
        auditAfter.put("restoredRecruitments", restoreResult.restoredRecruitments());
        auditAfter.put("restoredSlots", restoreResult.restoredSlots());
        auditAfter.put("restoredPlans", restoreResult.restoredPlans());
        auditAfter.put("reason", reason);
        auditLogService.recordAudit(userId, "TEAM_REOPENED", "TEAM", teamId, auditPayload(before), auditAfter);
        Map<String, Object> operationPayload = new LinkedHashMap<>();
        operationPayload.put("teamId", teamId);
        operationPayload.put("closureSnapshotId", snapshot == null ? null : longValue(snapshot.get("closureSnapshotId")));
        operationPayload.put("restoreSnapshotYn", restoreSnapshot ? "Y" : "N");
        operationPayload.put("teamStatus", restoreResult.teamStatus());
        operationPayload.put("restoredRecruitments", restoreResult.restoredRecruitments());
        operationPayload.put("restoredSlots", restoreResult.restoredSlots());
        operationPayload.put("restoredPlans", restoreResult.restoredPlans());
        auditLogService.recordOperation(
                "INFO",
                "TEAM_REOPENED",
                "종료된 팀이 재개되었습니다. 지원/초대 취소 이력은 자동 복구하지 않았습니다.",
                operationPayload
        );
        return after;
    }

    public List<Map<String, Object>> planItems(Long userId, Long teamId) {
        assertActiveMember(userId, teamId);
        return teamMapper.selectPlanItemsByTeamId(teamId);
    }

    @Transactional
    public Map<String, Object> createPlanItem(Long userId, Long teamId, PlanItemRequest request) {
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> plan = planMap(null, teamId, userId, request);
        assertAssigneeInTeam(teamId, longValue(plan.get("assigneeUserId")));
        teamMapper.insertPlanItem(plan);
        Map<String, Object> created = teamMapper.selectPlanItemById(longValue(plan.get("planItemId")));
        notifyAssignee(userId, created, "새 팀 계획이 배정되었습니다.");
        auditLogService.recordAudit(userId, "TEAM_PLAN_CREATED", "TEAM_PLAN", longValue(created.get("planItemId")), null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> updatePlanItem(Long userId, Long planItemId, PlanItemRequest request) {
        Map<String, Object> before = requirePlanItem(planItemId);
        Long teamId = longValue(before.get("teamId"));
        assertManager(userId, teamId);
        assertTeamOpen(teamId);
        Map<String, Object> plan = planMap(planItemId, teamId, userId, request);
        assertAssigneeInTeam(teamId, longValue(plan.get("assigneeUserId")));
        teamMapper.updatePlanItem(plan);
        Map<String, Object> after = teamMapper.selectPlanItemById(planItemId);
        notifyAssignee(userId, after, "팀 계획이 수정되었습니다.");
        auditLogService.recordAudit(userId, "TEAM_PLAN_UPDATED", "TEAM_PLAN", planItemId, before, after);
        return after;
    }

    @Transactional
    public Map<String, Object> updatePlanStatus(Long userId, Long planItemId, PlanStatusRequest request) {
        Map<String, Object> before = requirePlanItem(planItemId);
        Long teamId = longValue(before.get("teamId"));
        assertActiveMember(userId, teamId);
        assertTeamOpen(teamId);
        boolean manager = isManager(userId, teamId);
        Long assigneeUserId = longValue(before.get("assigneeUserId"));
        if (!manager && !Objects.equals(assigneeUserId, userId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "담당자 또는 팀 관리자만 상태를 변경할 수 있습니다.");
        }
        String status = normalizePlanStatus(request.status());
        teamMapper.updatePlanItemStatus(planItemId, status);
        Map<String, Object> after = teamMapper.selectPlanItemById(planItemId);
        auditLogService.recordAudit(userId, "TEAM_PLAN_STATUS_UPDATED", "TEAM_PLAN", planItemId, before, after);
        return after;
    }

    private void activateMember(Long teamId, Long userId, Map<String, Object> requestRow) {
        teamMapper.upsertActiveTeamMember(teamId, userId, "MEMBER");
        if (teamMapper.incrementSlotAcceptedCount(longValue(requestRow.get("slotId"))) == 0) {
            throw new SlateException("모집 슬롯 정원이 이미 마감되었습니다.");
        }
        teamMapper.closeFilledSlot(longValue(requestRow.get("slotId")));
        teamMapper.recountTeamMemberCount(teamId);
    }

    private void assertCanAddMember(Long teamId, Long userId) {
        Map<String, Object> team = team(teamId);
        Map<String, Object> member = teamMapper.selectTeamMember(teamId, userId);
        if (member != null && "ACTIVE".equals(member.get("status"))) {
            throw new SlateException("이미 팀에 참여 중인 사용자입니다.");
        }
        if (teamMapper.countActiveTeamsByUserId(userId) >= 3) {
            throw new SlateException("대상 사용자는 참여 중인 팀이 이미 3개입니다.");
        }
        if (longValue(team.get("currentMemberCount")) >= longValue(team.get("maxMemberCount"))) {
            throw new SlateException("팀 최대 인원을 초과할 수 없습니다.");
        }
    }

    private Map<String, Object> enrich(Map<String, Object> team) {
        Long teamId = ((Number) team.get("teamId")).longValue();
        Map<String, Object> result = new LinkedHashMap<>(team);
        result.putIfAbsent("imageUrl", null);
        result.put("genres", teamMapper.selectTeamGenres(teamId));
        result.put("members", teamMapper.selectTeamMembers(teamId));
        return result;
    }

    private Map<String, Object> closureSnapshotPayload(Long userId, Long teamId, String endType, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> team = team(teamId);
        payload.put("team", team);
        payload.put("endType", endType);
        payload.put("reason", reason.trim());
        payload.put("createdBy", userId);
        List<Map<String, Object>> recruitmentRows = recruitments(teamId);
        payload.put("recruitments", recruitmentRows);
        payload.put("plans", teamMapper.selectPlanItemsByTeamId(teamId));
        payload.put("applications", teamMapper.selectApplicationsByTeamId(teamId));
        payload.put("invitations", teamMapper.selectInvitationsByTeamId(teamId));
        payload.put("summary", Map.of(
                "memberCount", ((List<?>) team.get("members")).size(),
                "recruitmentCount", recruitmentRows.size(),
                "planCount", teamMapper.selectPlanItemsByTeamId(teamId).size()
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
        String snapshotJson = snapshot == null ? null : stringValue(snapshot.get("snapshotJson"));
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

    private RestoreResult restoreFromSnapshot(Long teamId, Map<String, Object> snapshotPayload, boolean restoreSnapshot) {
        String teamStatus = restoredTeamStatus(snapshotPayload);
        int restoredRecruitments = 0;
        int restoredSlots = 0;
        int restoredPlans = 0;
        if (!restoreSnapshot || snapshotPayload.isEmpty()) {
            return new RestoreResult(teamStatus, restoredRecruitments, restoredSlots, restoredPlans);
        }
        for (Map<String, Object> recruitment : mapList(snapshotPayload.get("recruitments"))) {
            Long recruitmentId = longValue(recruitment.get("recruitmentId"));
            if (recruitmentId == null) {
                continue;
            }
            restoredRecruitments += teamMapper.restoreRecruitmentStatus(teamId, recruitmentId, normalizeRecruitmentStatus(recruitment.get("status")));
            for (Map<String, Object> slot : mapList(recruitment.get("slots"))) {
                Long slotId = longValue(slot.get("slotId"));
                if (slotId != null) {
                    restoredSlots += teamMapper.restoreSlotStatus(teamId, slotId, normalizeSlotStatus(slot.get("status")));
                }
            }
        }
        for (Map<String, Object> plan : mapList(snapshotPayload.get("plans"))) {
            Long planItemId = longValue(plan.get("planItemId"));
            if (planItemId != null) {
                restoredPlans += teamMapper.restorePlanItemStatus(teamId, planItemId, normalizePlanStatus(stringValue(plan.get("status"))));
            }
        }
        return new RestoreResult(teamStatus, restoredRecruitments, restoredSlots, restoredPlans);
    }

    private String restoredTeamStatus(Map<String, Object> snapshotPayload) {
        Map<String, Object> team = mapValue(snapshotPayload.get("team"));
        String status = textOrDefault(stringValue(team.get("status")), "RECRUITMENT_CLOSED").toUpperCase(Locale.ROOT);
        return TEAM_STATUSES.contains(status) ? status : "RECRUITMENT_CLOSED";
    }

    private void notifyTeamClosed(Long actorUserId, Map<String, Object> team, String endType, String reason) {
        Long teamId = longValue(team.get("teamId"));
        String title = "NORMAL".equals(endType) ? "팀이 종료되었습니다." : "팀이 해체되었습니다.";
        String body = textOrDefault((String) team.get("name"), "팀") + " 상태가 종료로 변경되었습니다. 사유: " + reason.trim();
        for (Map<String, Object> member : teamMapper.selectTeamMembers(teamId)) {
            Long recipientUserId = longValue(member.get("userId"));
            if (recipientUserId == null || recipientUserId.equals(actorUserId) || !"ACTIVE".equals(member.get("status"))) {
                continue;
            }
            notificationService.send(recipientUserId, actorUserId, "TEAM", title, body, "TEAM", teamId);
        }
    }

    private void notifyActiveTeamMembers(Long actorUserId, Long teamId, String title, String body) {
        for (Map<String, Object> member : teamMapper.selectTeamMembers(teamId)) {
            Long recipientUserId = longValue(member.get("userId"));
            if (recipientUserId == null || recipientUserId.equals(actorUserId) || !"ACTIVE".equals(member.get("status"))) {
                continue;
            }
            notificationService.send(recipientUserId, actorUserId, "TEAM", title, body, "TEAM", teamId);
        }
    }

    private Map<String, Object> teamMap(Long userId, Long teamId, TeamRequest request) {
        Map<String, Object> team = new LinkedHashMap<>();
        String regionAnyYn = yn(request.regionAnyYn());
        team.put("teamId", teamId);
        team.put("leaderUserId", userId);
        team.put("name", request.name().trim());
        team.put("description", request.description().trim());
        team.put("status", normalizeTeamStatus(request.status()));
        team.put("regionAnyYn", regionAnyYn);
        team.put("regionId", "Y".equals(regionAnyYn) ? null : request.regionId());
        team.put("expectedDuration", request.expectedDuration());
        team.put("maxMemberCount", request.maxMemberCount() == null ? 100 : request.maxMemberCount());
        return team;
    }

    private Map<String, Object> recruitmentMap(Long recruitmentId, Long teamId, Long userId, RecruitmentRequest request) {
        Map<String, Object> recruitment = new LinkedHashMap<>();
        recruitment.put("recruitmentId", recruitmentId);
        recruitment.put("teamId", teamId);
        recruitment.put("title", request.title().trim());
        String deadlineAt = textOrNull(request.deadlineAt());
        recruitment.put("status", normalizeRecruitmentStatusForDeadline(request.status(), deadlineAt));
        recruitment.put("deadlineAt", deadlineAt);
        recruitment.put("workStartAt", textOrNull(request.workStartAt()));
        recruitment.put("createdBy", userId);
        return recruitment;
    }

    private Map<String, Object> slotMap(Long slotId, Long recruitmentId, SlotRequest request) {
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("slotId", slotId);
        slot.put("recruitmentId", recruitmentId);
        slot.put("roleId", request.roleId());
        slot.put("requiredCount", request.requiredCount() == null ? 1 : request.requiredCount());
        slot.put("requiredExperienceLevel", request.requiredExperienceLevel());
        slot.put("collaborationCondition", request.collaborationCondition());
        slot.put("requiredYn", yn(textOrDefault(request.requiredYn(), "Y")));
        slot.put("roleDuration", textOrNull(request.roleDuration()));
        slot.put("equipmentRequiredYn", textOrNull(request.equipmentRequiredYn()));
        slot.put("status", textOrDefault(request.status(), "OPEN"));
        return slot;
    }

    private Map<String, Object> planMap(Long planItemId, Long teamId, Long userId, PlanItemRequest request) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("planItemId", planItemId);
        plan.put("teamId", teamId);
        plan.put("title", request.title().trim());
        plan.put("description", textOrNull(request.description()));
        plan.put("assigneeUserId", request.assigneeUserId());
        plan.put("roleId", request.roleId());
        plan.put("dueAt", textOrNull(request.dueAt()));
        plan.put("status", normalizePlanStatus(textOrDefault(request.status(), "TODO")));
        plan.put("createdBy", userId);
        return plan;
    }

    private void replaceTeamGenres(Long teamId, List<Long> genreIds) {
        teamMapper.deleteTeamGenres(teamId);
        for (Long genreId : genreIds) {
            teamMapper.insertTeamGenre(teamId, genreId);
        }
    }

    private void assertLeader(Long userId, Long teamId) {
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        if (!"LEADER".equals(role)) {
            throw new SlateException("팀장만 팀 정보를 수정할 수 있습니다.");
        }
    }

    private void assertTeamOpen(Long teamId) {
        Map<String, Object> team = teamMapper.selectTeamById(teamId);
        if (team == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다.");
        }
        if ("ENDED".equals(team.get("status"))) {
            throw new SlateException("이미 종료된 팀입니다.");
        }
    }

    private void assertManager(Long userId, Long teamId) {
        if (!isManager(userId, teamId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀장 또는 부팀장 권한이 필요합니다.");
        }
    }

    private boolean isManager(Long userId, Long teamId) {
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        return "LEADER".equals(role) || "SUB_LEADER".equals(role);
    }

    private void assertActiveMember(Long userId, Long teamId) {
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        if (!StringUtils.hasText(role)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀 멤버만 접근할 수 있습니다.");
        }
    }

    private void assertAssigneeInTeam(Long teamId, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return;
        }
        Map<String, Object> member = teamMapper.selectTeamMember(teamId, assigneeUserId);
        if (member == null || !"ACTIVE".equals(member.get("status"))) {
            throw new SlateException("담당자는 팀 멤버여야 합니다.");
        }
    }

    private Map<String, Object> requireApplication(Long applicationId) {
        Map<String, Object> application = teamMapper.selectApplicationById(applicationId);
        if (application == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀 지원을 찾을 수 없습니다.");
        }
        return application;
    }

    private Map<String, Object> requireInvitation(Long invitationId) {
        Map<String, Object> invitation = teamMapper.selectInvitationById(invitationId);
        if (invitation == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀 초대를 찾을 수 없습니다.");
        }
        return invitation;
    }

    private Map<String, Object> requireTeamMember(Long teamId, Long userId) {
        Map<String, Object> member = teamMapper.selectTeamMember(teamId, userId);
        if (member == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀원을 찾을 수 없습니다.");
        }
        return member;
    }

    private Map<String, Object> requirePlanItem(Long planItemId) {
        Map<String, Object> plan = teamMapper.selectPlanItemById(planItemId);
        if (plan == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀 계획을 찾을 수 없습니다.");
        }
        return plan;
    }

    private void notifyAssignee(Long senderUserId, Map<String, Object> plan, String title) {
        Long assigneeUserId = longValue(plan.get("assigneeUserId"));
        if (assigneeUserId == null || assigneeUserId.equals(senderUserId)) {
            return;
        }
        notificationService.send(assigneeUserId, senderUserId, "TEAM", title, textOrDefault((String) plan.get("title"), "팀 계획"), "TEAM", longValue(plan.get("teamId")));
    }

    private String decision(String value) {
        String decision = textOrDefault(value, "").toUpperCase(Locale.ROOT);
        if (!Set.of("ACCEPTED", "REJECTED").contains(decision)) {
            throw new SlateException("decision은 ACCEPTED 또는 REJECTED만 가능합니다.");
        }
        return decision;
    }

    private String normalizeTeamRole(String value) {
        String role = textOrDefault(value, "MEMBER").toUpperCase(Locale.ROOT);
        if (!TEAM_ROLES.contains(role)) {
            throw new SlateException("올바르지 않은 팀 권한입니다.");
        }
        return role;
    }

    private String normalizePlanStatus(String value) {
        String status = textOrDefault(value, "TODO").toUpperCase(Locale.ROOT);
        if (!PLAN_STATUSES.contains(status)) {
            throw new SlateException("올바르지 않은 계획 상태입니다.");
        }
        return status;
    }

    private String normalizeTeamStatus(String value) {
        String status = textOrDefault(value, "RECRUITING").toUpperCase(Locale.ROOT);
        if (!TEAM_STATUSES.contains(status)) {
            throw new SlateException("팀 상태는 종료 API가 아닌 팀 종료 기능으로만 ENDED 처리할 수 있습니다.");
        }
        return status;
    }

    private String normalizeRecruitmentStatus(Object value) {
        String status = textOrDefault(stringValue(value), "CLOSED").toUpperCase(Locale.ROOT);
        return RECRUITMENT_STATUSES.contains(status) ? status : "CLOSED";
    }

    private String normalizeRecruitmentStatusForDeadline(String value, String deadlineAt) {
        String status = textOrDefault(value, "OPEN").toUpperCase(Locale.ROOT);
        if (!RECRUITMENT_STATUSES.contains(status)) {
            status = "CLOSED";
        }
        if ("OPEN".equals(status) && isPastDeadline(deadlineAt)) {
            return "CLOSED";
        }
        return status;
    }

    private boolean isPastDeadline(String deadlineAt) {
        if (!StringUtils.hasText(deadlineAt)) {
            return false;
        }
        String normalized = deadlineAt.trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized).isBefore(LocalDateTime.now());
        } catch (DateTimeParseException firstFailure) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).isBefore(LocalDateTime.now());
            } catch (DateTimeParseException ignored) {
                return false;
            }
        }
    }

    private String normalizeSlotStatus(Object value) {
        String status = textOrDefault(stringValue(value), "CLOSED").toUpperCase(Locale.ROOT);
        return SLOT_STATUSES.contains(status) ? status : "CLOSED";
    }

    private String normalizeEndType(String value) {
        String endType = textOrDefault(value, "NORMAL").toUpperCase(Locale.ROOT);
        if (!TEAM_END_TYPES.contains(endType)) {
            throw new SlateException("endType은 NORMAL 또는 DISSOLUTION만 가능합니다.");
        }
        return endType;
    }

    private String yn(String value) {
        String text = textOrDefault(value, "N");
        return "Y".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text) ? "Y" : "N";
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

    private String stringValue(Object value) {
        return value == null ? null : Objects.toString(value, null);
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
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

    private Map<String, Object> auditPayload(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("teamId", "teamMemberId", "userId", "name", "status", "teamRole", "currentMemberCount", "maxMemberCount")) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        return result;
    }

    private record RestoreResult(
            String teamStatus,
            int restoredRecruitments,
            int restoredSlots,
            int restoredPlans
    ) {
    }
}
