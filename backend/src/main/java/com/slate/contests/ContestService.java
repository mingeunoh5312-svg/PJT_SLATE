package com.slate.contests;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.contests.AdminContestController.ContestDeleteRequest;
import com.slate.contests.AdminContestController.ContestRequestDecisionRequest;
import com.slate.contests.AdminContestController.ContestStatusRequest;
import com.slate.contests.ContestController.FitRequest;
import com.slate.contests.ContestController.PrepareRequest;
import com.slate.notifications.NotificationService;
import com.slate.media.MediaImageService;
import com.slate.operations.AuditLogService;
import com.slate.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContestService {

    private static final List<String> DEFAULT_CHECKLIST = List.of(
            "공모 요강과 제출 자격 확인",
            "대표 작업물 링크 또는 포트폴리오 정리",
            "팀/프로필 소개 문장 준비",
            "제출 이메일 또는 외부 제출 링크 확인"
    );
    private static final List<String> CRAWL_SOURCE_METADATA_FIELDS = List.of(
            "posterSourceType",
            "posterOriginalUrl",
            "posterCollectedAt",
            "sourceName",
            "sourceExternalId",
            "sourceUrl",
            "sourceCategoryCode",
            "sourceCollectedAt",
            "sourceUpdatedAt",
            "sourcePermissionText",
            "sourceAttribution"
    );

    private final ContestMapper contestMapper;
    private final AuditLogService auditLogService;
    private final AdminPermissionService adminPermissionService;
    private final NotificationService notificationService;
    private final MediaImageService mediaImageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContestService(
            ContestMapper contestMapper,
            AuditLogService auditLogService,
            AdminPermissionService adminPermissionService,
            NotificationService notificationService,
            MediaImageService mediaImageService
    ) {
        this.contestMapper = contestMapper;
        this.auditLogService = auditLogService;
        this.adminPermissionService = adminPermissionService;
        this.notificationService = notificationService;
        this.mediaImageService = mediaImageService;
    }

    public List<Map<String, Object>> contests(
            String status,
            String sort,
            String contestType,
            String keyword,
            List<String> targetCodes,
            List<String> regionCodes,
            List<String> organizerTypes,
            Long totalPrizeMin,
            Long totalPrizeMax,
            Long firstPrizeMin,
            Long firstPrizeMax,
            Integer limit,
            Long userId
    ) {
        return contests(
                status,
                sort,
                contestType,
                keyword,
                null,
                targetCodes,
                regionCodes,
                organizerTypes,
                totalPrizeMin,
                totalPrizeMax,
                firstPrizeMin,
                firstPrizeMax,
                limit,
                userId
        );
    }

    public List<Map<String, Object>> contests(
            String status,
            String sort,
            String contestType,
            String keyword,
            Integer deadlineWithinDays,
            List<String> targetCodes,
            List<String> regionCodes,
            List<String> organizerTypes,
            Long totalPrizeMin,
            Long totalPrizeMax,
            Long firstPrizeMin,
            Long firstPrizeMax,
            Integer limit,
            Long userId
    ) {
        validateAmountRange(totalPrizeMin, totalPrizeMax, "총상금");
        validateAmountRange(firstPrizeMin, firstPrizeMax, "1등 상금");
        RegionFilter regionFilter = normalizeContestRegionFilter(regionCodes);
        ContestSearchCriteria filter = new ContestSearchCriteria(
                normalizeStatus(status),
                normalizeSort(sort),
                normalizeContestType(contestType),
                textOrNull(keyword),
                normalizeDeadlineWithinDays(deadlineWithinDays),
                regionFilter.mode(),
                normalizeCodes(targetCodes, ContestFilterCatalog.TARGETS, "대상"),
                regionFilter.codes(),
                normalizeCodes(organizerTypes, ContestFilterCatalog.ORGANIZER_TYPES, "주최 유형"),
                totalPrizeMin,
                totalPrizeMax,
                firstPrizeMin,
                firstPrizeMax,
                safeLimit(limit)
        );
        return contestMapper.selectContests(filter, userId).stream().map(this::normalizeContestRow).toList();
    }

    public List<Map<String, Object>> urgentContests(Integer limit, Long userId) {
        return contestMapper.selectUrgentContests(userId, Math.max(1, Math.min(limit == null ? 4 : limit, 5)))
                .stream()
                .map(this::normalizeContestRow)
                .toList();
    }

    public Map<String, Object> contest(Long contestId, String basisType, Long basisId, Long userId) {
        String normalizedBasisType = normalizeBasisTypeOrNull(basisType);
        Long normalizedBasisId = normalizeBasisId(normalizedBasisType, basisId, userId);
        Map<String, Object> contest = contestMapper.selectContestById(contestId, normalizedBasisType, normalizedBasisId, userId);
        if (contest == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "공모전을 찾을 수 없습니다.");
        }
        Map<String, Object> result = normalizeContestRow(contest);
        if (userId != null && normalizedBasisType != null && normalizedBasisId != null) {
            result.put("preparation", normalizePreparation(contestMapper.selectPreparation(contestId, userId, normalizedBasisType, normalizedBasisId)));
        } else {
            result.put("preparation", null);
        }
        return result;
    }

    public Map<String, Object> bases(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", contestMapper.selectProfileBasis(userId));
        result.put("teams", contestMapper.selectTeamBases(userId));
        return result;
    }

    @Transactional
    public Map<String, Object> toggleSave(Long contestId, Long userId) {
        requireContest(contestId);
        boolean active = contestMapper.existsContestSave(contestId, userId) == 0;
        if (active) {
            contestMapper.insertContestSave(contestId, userId);
        } else {
            contestMapper.deleteContestSave(contestId, userId);
        }
        contestMapper.recountSaveCount(contestId);
        Number count = contestMapper.selectSaveCount(contestId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contestId", contestId);
        result.put("saved", active);
        result.put("saveCount", count == null ? 0 : count.intValue());
        auditLogService.recordAudit(userId, active ? "CONTEST_SAVED" : "CONTEST_UNSAVED", "CONTEST", contestId, null, result);
        return result;
    }

    @Transactional
    public Map<String, Object> calculateFit(Long contestId, Long userId, FitRequest request) {
        Map<String, Object> contest = requireContest(contestId);
        String basisType = normalizeBasisType(request.basisType());
        Long basisId = resolveAndAuthorizeBasisId(basisType, request.basisId(), userId);
        Map<String, Object> existing = contestMapper.selectFitCache(contestId, basisType, basisId);
        if (existing != null && isFresh(existing)) {
            Map<String, Object> cached = normalizeFit(existing);
            cached.put("cacheHit", true);
            return cached;
        }

        Map<String, Object> basis = basisContext(basisType, basisId, userId);
        Map<String, Object> calculated = calculateFallbackFit(contest, basisType, basis);
        calculated.put("contestId", contestId);
        calculated.put("basisType", basisType);
        calculated.put("basisId", basisId);
        calculated.put("status", "READY");
        calculated.put("reasonJson", toJson(calculated.get("reasons")));
        contestMapper.upsertFitCache(calculated);

        Map<String, Object> saved = normalizeFit(contestMapper.selectFitCache(contestId, basisType, basisId));
        saved.put("cacheHit", false);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("basisType", basisType);
        audit.put("basisId", basisId);
        audit.put("fitScore", saved.get("fitScore"));
        audit.put("calculationMethod", calculated.get("calculationMethod"));
        auditLogService.recordAudit(userId, "CONTEST_FIT_CALCULATED", "CONTEST", contestId, null, audit);
        return saved;
    }

    @Transactional
    public Map<String, Object> savePreparation(Long contestId, Long userId, PrepareRequest request) {
        requireContest(contestId);
        String basisType = normalizeBasisType(request.basisType());
        Long basisId = resolveAndAuthorizeBasisId(basisType, request.basisId(), userId);
        List<String> checklist = cleanChecklist(request.checklistItems());
        Map<String, Object> preparation = new LinkedHashMap<>();
        preparation.put("contestId", contestId);
        preparation.put("userId", userId);
        preparation.put("basisType", basisType);
        preparation.put("basisId", basisId);
        preparation.put("checklistJson", toJson(checklist));
        preparation.put("memo", textOrNull(request.memo()));
        contestMapper.upsertPreparation(preparation);
        Map<String, Object> saved = normalizePreparation(contestMapper.selectPreparation(contestId, userId, basisType, basisId));
        auditLogService.recordAudit(userId, "CONTEST_PREPARATION_SAVED", "CONTEST", contestId, null, Map.of(
                "basisType", basisType,
                "basisId", basisId,
                "checklistSize", checklist.size()
        ));
        return saved;
    }

    public List<Map<String, Object>> myOpenRequests(CurrentUser currentUser) {
        requireCompany(currentUser);
        return contestMapper.selectOpenRequestsByRequester(currentUser.userId()).stream()
                .map(this::normalizeOpenRequest)
                .toList();
    }

    @Transactional
    public Map<String, Object> createOpenRequest(CurrentUser currentUser, ContestPayloadRequest request) {
        requireCompany(currentUser);
        Map<String, Object> row = contestPayload(request, "INTERNAL", null, currentUser.userId(), null);
        row.put("requesterUserId", currentUser.userId());
        contestMapper.insertContestOpenRequest(row);
        Map<String, Object> created = requireOpenRequest(longValue(row.get("requestId")));
        auditLogService.recordAudit(
                currentUser.userId(),
                "CONTEST_OPEN_REQUEST_CREATED",
                "CONTEST_OPEN_REQUEST",
                longValue(created.get("requestId")),
                null,
                auditRequestPayload(created)
        );
        auditLogService.recordOperation(
                "INFO",
                "CONTEST_OPEN_REQUEST_CREATED",
                "회사 공모전 개설 요청이 접수되었습니다.",
                Map.of("requestId", created.get("requestId"), "requesterUserId", currentUser.userId())
        );
        return normalizeOpenRequest(created);
    }

    public List<Map<String, Object>> adminOpenRequests(Long adminUserId, String status, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        return contestMapper.selectContestOpenRequests(normalizeRequestStatus(status), safeAdminLimit(limit)).stream()
                .map(this::normalizeOpenRequest)
                .toList();
    }

    public List<Map<String, Object>> adminManagedContests(
            Long adminUserId,
            String status,
            String contestType,
            Long requesterCompanyUserId,
            Integer limit
    ) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        return contestMapper.selectManagedContests(
                normalizeStatus(status),
                normalizeContestType(contestType),
                requesterCompanyUserId,
                null,
                null,
                adminUserId,
                safeAdminLimit(limit)
        ).stream().map(this::normalizeContestRow).toList();
    }

    @Transactional
    public Map<String, Object> adminCreateContest(Long adminUserId, ContestPayloadRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        Map<String, Object> row = contestPayload(request, normalizeContestTypeForWrite(request.contestType()), adminUserId, null, null);
        contestMapper.insertContest(row);
        Long contestId = longValue(row.get("contestId"));
        Map<String, Object> created = requireContest(contestId);
        auditLogService.recordAudit(adminUserId, "CONTEST_CREATED", "CONTEST", contestId, null, created);
        auditLogService.recordOperation(
                "INFO",
                "CONTEST_CREATED",
                "관리자가 공모전을 등록했습니다.",
                Map.of("contestId", contestId, "contestType", created.get("contestType"))
        );
        return normalizeContestRow(created);
    }

    @Transactional
    public Map<String, Object> adminUpdateContest(Long adminUserId, Long contestId, ContestPayloadRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        Map<String, Object> before = requireContest(contestId);
        Map<String, Object> row = contestPayload(
                request,
                normalizeContestTypeForWrite(request.contestType()),
                longValue(before.get("createdBy")),
                longValue(before.get("requesterCompanyUserId")),
                longValue(before.get("sourceRequestId"))
        );
        row.put("contestId", contestId);
        row.put("status", before.get("status"));
        contestMapper.updateContest(row);
        Map<String, Object> updated = requireContest(contestId);
        auditLogService.recordAudit(adminUserId, "CONTEST_UPDATED", "CONTEST", contestId, auditPayload(before), auditPayload(updated));
        auditLogService.recordOperation(
                "INFO",
                "CONTEST_UPDATED",
                "관리자가 공모전을 수정했습니다.",
                operationPayload("contestId", contestId, "actorUserId", adminUserId, "status", updated.get("status"))
        );
        notifyCompanyContestOwner(updated, adminUserId, "공모전 정보가 수정되었습니다.", "관리자가 승인된 공모전 정보를 수정했습니다.", "CONTEST_UPDATED");
        return normalizeContestRow(updated);
    }

    @Transactional
    public Map<String, Object> adminUpdateContestStatus(Long adminUserId, Long contestId, ContestStatusRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        Map<String, Object> before = requireContest(contestId);
        String status = normalizeContestStatusForWrite(request.status(), true);
        contestMapper.updateContestStatus(contestId, status);
        Map<String, Object> updated = requireContest(contestId);
        auditLogService.recordAudit(adminUserId, "CONTEST_STATUS_UPDATED", "CONTEST", contestId, auditPayload(before), auditPayload(updated));
        auditLogService.recordOperation(
                "INFO",
                "CONTEST_STATUS_UPDATED",
                "관리자가 공모전 상태를 변경했습니다.",
                operationPayload("contestId", contestId, "status", status, "reason", textOrDefault(request.reason(), "관리자 상태 변경"))
        );
        notifyCompanyContestOwner(updated, adminUserId, statusTitle(status), textOrDefault(request.reason(), "관리자가 공모전 상태를 변경했습니다."), "CONTEST_STATUS");
        return normalizeContestRow(updated);
    }

    @Transactional
    public Map<String, Object> adminDeleteContests(Long adminUserId, ContestDeleteRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        List<Long> contestIds = normalizeContestIds(request == null ? null : request.contestIds());
        String reason = textOrDefault(request == null ? null : request.reason(), "관리자 선택 삭제");

        List<Map<String, Object>> deleted = new ArrayList<>();
        for (Long contestId : contestIds) {
            Map<String, Object> before = requireContest(contestId);
            contestMapper.clearContestOpenRequestApprovedContest(contestId);
            contestMapper.deleteContestSaves(contestId);
            contestMapper.deleteContestFitCaches(contestId);
            contestMapper.deleteContestPreparations(contestId);
            if (contestMapper.deleteContestById(contestId) == 0) {
                throw new SlateException(HttpStatus.NOT_FOUND, "공모전을 찾을 수 없습니다.");
            }
            mediaImageService.deleteStoredAfterCommit(text(before.get("representativeImagePath")));
            auditLogService.recordAudit(adminUserId, "CONTEST_DELETED", "CONTEST", contestId, auditPayload(before), null);
            deleted.add(auditPayload(before));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedCount", contestIds.size());
        result.put("deletedCount", deleted.size());
        result.put("contestIds", contestIds);
        result.put("reason", reason);
        auditLogService.recordOperation(
                "WARN",
                "CONTEST_DELETED",
                "관리자가 공모전을 삭제했습니다.",
                Map.of("contestIds", contestIds, "deletedCount", deleted.size(), "reason", reason)
        );
        return result;
    }

    public List<Map<String, Object>> myManagedContests(CurrentUser currentUser) {
        requireCompany(currentUser);
        return contestMapper.selectManagedContests(
                "ALL",
                null,
                currentUser.userId(),
                null,
                null,
                currentUser.userId(),
                50
        ).stream().map(this::normalizeContestRow).toList();
    }

    @Transactional
    public Map<String, Object> updateCompanyManagedContest(CurrentUser currentUser, Long contestId, ContestPayloadRequest request) {
        requireCompany(currentUser);
        Map<String, Object> before = requireOwnedCompanyContest(currentUser.userId(), contestId);
        if (!"OPEN".equals(before.get("status"))) {
            throw new SlateException("진행 중인 회사 공모전만 수정할 수 있습니다.");
        }
        Map<String, Object> row = contestPayload(
                request,
                "INTERNAL",
                longValue(before.get("createdBy")),
                currentUser.userId(),
                longValue(before.get("sourceRequestId"))
        );
        row.put("contestId", contestId);
        row.put("status", before.get("status"));
        contestMapper.updateContest(row);
        Map<String, Object> updated = requireContest(contestId);
        auditLogService.recordAudit(currentUser.userId(), "COMPANY_CONTEST_UPDATED", "CONTEST", contestId, auditPayload(before), auditPayload(updated));
        auditLogService.recordOperation(
                "INFO",
                "COMPANY_CONTEST_UPDATED",
                "회사 계정이 승인된 공모전을 수정했습니다.",
                operationPayload("contestId", contestId, "requesterCompanyUserId", currentUser.userId(), "status", updated.get("status"))
        );
        return normalizeContestRow(updated);
    }

    @Transactional
    public Map<String, Object> updateCompanyManagedContestStatus(CurrentUser currentUser, Long contestId, ContestStatusRequest request) {
        requireCompany(currentUser);
        Map<String, Object> before = requireOwnedCompanyContest(currentUser.userId(), contestId);
        String status = normalizeContestStatusForWrite(request.status(), false);
        contestMapper.updateContestStatus(contestId, status);
        Map<String, Object> updated = requireContest(contestId);
        auditLogService.recordAudit(currentUser.userId(), "COMPANY_CONTEST_STATUS_UPDATED", "CONTEST", contestId, auditPayload(before), auditPayload(updated));
        auditLogService.recordOperation(
                "INFO",
                "COMPANY_CONTEST_STATUS_UPDATED",
                "회사 계정이 승인된 공모전 상태를 변경했습니다.",
                operationPayload("contestId", contestId, "status", status, "requesterCompanyUserId", currentUser.userId())
        );
        return normalizeContestRow(updated);
    }

    @Transactional
    public Map<String, Object> decideOpenRequest(Long adminUserId, Long requestId, ContestRequestDecisionRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        Map<String, Object> before = requireOpenRequest(requestId);
        if (!"PENDING".equals(before.get("status"))) {
            throw new SlateException("검토 대기 상태의 요청만 처리할 수 있습니다.");
        }
        String decision = normalizeDecision(request.decision());
        Long approvedContestId = null;
        if ("APPROVED".equals(decision)) {
            Map<String, Object> contest = contestFromOpenRequest(before, adminUserId);
            contestMapper.insertContest(contest);
            approvedContestId = longValue(contest.get("contestId"));
            if (StringUtils.hasText(text(before.get("representativeImagePath")))) {
                contestMapper.clearContestOpenRequestImagePath(requestId);
            }
        } else if (StringUtils.hasText(text(before.get("representativeImagePath")))) {
            contestMapper.clearContestOpenRequestImagePath(requestId);
            mediaImageService.deleteStoredAfterCommit(text(before.get("representativeImagePath")));
        }
        contestMapper.updateContestOpenRequestDecision(requestId, decision, textOrNull(request.reason()), adminUserId, approvedContestId);
        Map<String, Object> decided = requireOpenRequest(requestId);
        Long requesterUserId = longValue(before.get("requesterUserId"));
        String title = "APPROVED".equals(decision) ? "공모전 개설 요청이 승인되었습니다." : "공모전 개설 요청이 거절되었습니다.";
        String body = "APPROVED".equals(decision)
                ? "요청한 공모전이 Slate 공모전 목록에 등록되었습니다."
                : "요청 보완이 필요합니다. 사유: " + textOrDefault(request.reason(), "관리자 검토 결과");
        notificationService.send(requesterUserId, adminUserId, "ADMIN", title, body, "CONTEST_OPEN_REQUEST", requestId);
        auditLogService.recordAudit(
                adminUserId,
                "CONTEST_OPEN_REQUEST_" + decision,
                "CONTEST_OPEN_REQUEST",
                requestId,
                auditRequestPayload(before),
                auditRequestPayload(decided)
        );
        auditLogService.recordOperation(
                "INFO",
                "CONTEST_OPEN_REQUEST_DECISION",
                "공모전 개설 요청 검토 결과가 저장되었습니다.",
                operationPayload("requestId", requestId, "decision", decision, "approvedContestId", approvedContestId)
        );
        return normalizeOpenRequest(decided);
    }

    private Map<String, Object> requireContest(Long contestId) {
        Map<String, Object> contest = contestMapper.selectContestById(contestId, null, null, null);
        if (contest == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "공모전을 찾을 수 없습니다.");
        }
        return contest;
    }

    private Map<String, Object> requireOwnedCompanyContest(Long userId, Long contestId) {
        Map<String, Object> contest = requireContest(contestId);
        if (!userId.equals(longValue(contest.get("requesterCompanyUserId")))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "승인받은 회사 공모전만 관리할 수 있습니다.");
        }
        return contest;
    }

    private Map<String, Object> requireOpenRequest(Long requestId) {
        Map<String, Object> request = contestMapper.selectContestOpenRequestById(requestId);
        if (request == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "공모전 개설 요청을 찾을 수 없습니다.");
        }
        return request;
    }

    private void requireCompany(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!"COMPANY".equals(currentUser.accountType())) {
            throw new SlateException(HttpStatus.FORBIDDEN, "회사 계정만 공모전 개설을 요청할 수 있습니다.");
        }
    }

    private Map<String, Object> contestPayload(
            ContestPayloadRequest request,
            String contestType,
            Long createdBy,
            Long requesterCompanyUserId,
            Long sourceRequestId
    ) {
        String normalizedType = normalizeContestTypeForWrite(contestType);
        LocalDateTime startAt = parseDateTimeOrNull(request.startAt(), "시작일 형식이 올바르지 않습니다.");
        LocalDateTime deadlineAt = parseRequiredDateTime(request.deadlineAt(), "마감일 형식이 올바르지 않습니다.");
        if (deadlineAt.isBefore(LocalDateTime.now())) {
            throw new SlateException("마감일은 현재 시각 이후여야 합니다.");
        }
        if (startAt != null && startAt.isAfter(deadlineAt)) {
            throw new SlateException("시작일은 마감일보다 늦을 수 없습니다.");
        }
        String submissionEmail = textOrNull(request.submissionEmail());
        String externalUrl = textOrNull(request.externalUrl());
        if ("INTERNAL".equals(normalizedType) && !StringUtils.hasText(submissionEmail)) {
            throw new SlateException("자체 공모전은 제출 이메일이 필요합니다.");
        }
        if ("EXTERNAL".equals(normalizedType) && !StringUtils.hasText(externalUrl)) {
            throw new SlateException("외부 공모전은 외부 제출 링크가 필요합니다.");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("contestType", normalizedType);
        row.put("title", request.title().trim());
        row.put("summary", request.summary().trim());
        row.put("theme", textOrNull(request.theme()));
        row.put("prizeText", textOrNull(request.prizeText()));
        validatePrizeAmounts(request.totalPrizeAmount(), request.firstPrizeAmount());
        row.put("totalPrizeAmount", request.totalPrizeAmount());
        row.put("firstPrizeAmount", request.firstPrizeAmount());
        row.put("organizer", request.organizer().trim());
        row.put("organizerType", normalizeSingleCode(request.organizerType(), ContestFilterCatalog.ORGANIZER_TYPES, "주최 유형"));
        row.put("representativeImageUrl", normalizeExternalImageUrl(request.representativeImageUrl()));
        row.put("representativeImagePath", null);
        putEmptyCrawlSourceMetadata(row);
        row.put("submissionEmail", submissionEmail);
        row.put("externalUrl", externalUrl);
        row.put("targetText", textOrNull(request.targetText()));
        row.put("targetCodesJson", codesJson(request.targetCodes(), ContestFilterCatalog.TARGETS, "대상"));
        row.put("regionCodesJson", codesJson(request.regionCodes(), ContestFilterCatalog.REGIONS, "지역"));
        row.put("requiredRolesText", textOrNull(request.requiredRolesText()));
        row.put("relatedGenresText", textOrNull(request.relatedGenresText()));
        row.put("startAt", formatDateTime(startAt));
        row.put("deadlineAt", formatDateTime(deadlineAt));
        row.put("status", "OPEN");
        row.put("createdBy", createdBy);
        row.put("requesterCompanyUserId", requesterCompanyUserId);
        row.put("sourceRequestId", sourceRequestId);
        return row;
    }

    private void notifyCompanyContestOwner(Map<String, Object> contest, Long adminUserId, String title, String body, String targetType) {
        Long requesterCompanyUserId = longValue(contest.get("requesterCompanyUserId"));
        if (requesterCompanyUserId == null) {
            return;
        }
        notificationService.send(requesterCompanyUserId, adminUserId, "ADMIN", title, body, targetType, longValue(contest.get("contestId")));
    }

    private Map<String, Object> contestFromOpenRequest(Map<String, Object> request, Long adminUserId) {
        Map<String, Object> contest = new LinkedHashMap<>();
        contest.put("contestType", request.get("contestType"));
        contest.put("title", request.get("title"));
        contest.put("summary", request.get("summary"));
        contest.put("theme", request.get("theme"));
        contest.put("prizeText", request.get("prizeText"));
        contest.put("totalPrizeAmount", request.get("totalPrizeAmount"));
        contest.put("firstPrizeAmount", request.get("firstPrizeAmount"));
        contest.put("organizer", request.get("organizer"));
        contest.put("organizerType", request.get("organizerType"));
        contest.put("representativeImageUrl", request.get("representativeImageUrl"));
        contest.put("representativeImagePath", request.get("representativeImagePath"));
        putEmptyCrawlSourceMetadata(contest);
        contest.put("submissionEmail", request.get("submissionEmail"));
        contest.put("externalUrl", request.get("externalUrl"));
        contest.put("targetText", request.get("targetText"));
        contest.put("targetCodesJson", request.get("targetCodesJson"));
        contest.put("regionCodesJson", request.get("regionCodesJson"));
        contest.put("requiredRolesText", request.get("requiredRolesText"));
        contest.put("relatedGenresText", request.get("relatedGenresText"));
        contest.put("startAt", request.get("startAt"));
        contest.put("deadlineAt", request.get("deadlineAt"));
        contest.put("status", "OPEN");
        contest.put("createdBy", adminUserId);
        contest.put("requesterCompanyUserId", longValue(request.get("requesterUserId")));
        contest.put("sourceRequestId", longValue(request.get("requestId")));
        return contest;
    }

    private void putEmptyCrawlSourceMetadata(Map<String, Object> row) {
        CRAWL_SOURCE_METADATA_FIELDS.forEach(field -> row.put(field, null));
    }

    private Map<String, Object> basisContext(String basisType, Long basisId, Long userId) {
        Map<String, Object> basis = "TEAM".equals(basisType)
                ? contestMapper.selectTeamBasisById(basisId, userId)
                : contestMapper.selectProfileBasisById(basisId, userId);
        if (basis == null) {
            throw new SlateException(HttpStatus.FORBIDDEN, "적합도 산정 기준을 사용할 권한이 없습니다.");
        }
        return basis;
    }

    private Long resolveAndAuthorizeBasisId(String basisType, Long basisId, Long userId) {
        if (userId == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if ("TEAM".equals(basisType)) {
            if (basisId == null) {
                List<Map<String, Object>> teams = contestMapper.selectTeamBases(userId);
                if (teams.isEmpty()) {
                    throw new SlateException("참여 중인 팀이 없습니다.");
                }
                return ((Number) teams.get(0).get("teamId")).longValue();
            }
            if (contestMapper.selectTeamBasisById(basisId, userId) == null) {
                throw new SlateException(HttpStatus.FORBIDDEN, "팀 기준 적합도를 계산할 권한이 없습니다.");
            }
            return basisId;
        }

        Map<String, Object> profile = basisId == null
                ? contestMapper.selectProfileBasis(userId)
                : contestMapper.selectProfileBasisById(basisId, userId);
        if (profile == null) {
            throw new SlateException("프로필을 먼저 작성해주세요.");
        }
        return ((Number) profile.get("profileId")).longValue();
    }

    private Long normalizeBasisId(String basisType, Long basisId, Long userId) {
        if (basisType == null || userId == null) {
            return basisId;
        }
        try {
            return resolveAndAuthorizeBasisId(basisType, basisId, userId);
        } catch (SlateException ignored) {
            return null;
        }
    }

    private Map<String, Object> calculateFallbackFit(Map<String, Object> contest, String basisType, Map<String, Object> basis) {
        String contestText = text(contest.get("title")) + " " + text(contest.get("summary")) + " " + text(contest.get("theme"));
        String basisText = text(basis.get("title")) + " " + text(basis.get("summary")) + " " + text(basis.get("genreNames")) + " " + text(basis.get("roleNames"));
        int keywordMatches = keywordMatches(contestText, basisText);
        double score = 48 + Math.min(keywordMatches * 6, 24);
        List<String> reasons = new ArrayList<>();

        if ("TEAM".equals(basisType)) {
            score += 10;
            reasons.add("팀 구성과 모집 슬롯을 기준으로 제출 준비 가능성을 평가했습니다.");
        } else {
            score += 4;
            reasons.add("프로필의 역할, 장르, 활동 정보를 기준으로 평가했습니다.");
        }

        if ("INTERNAL".equals(contest.get("contestType"))) {
            score += 6;
            reasons.add("자체 공모전은 Slate 협업 흐름과 직접 연결됩니다.");
        }
        Number dDay = (Number) contest.get("dDay");
        if (dDay != null && dDay.intValue() >= 7) {
            score += 5;
            reasons.add("마감까지 제출 준비 시간을 확보할 수 있습니다.");
        } else if (dDay != null && dDay.intValue() >= 0) {
            score -= 4;
            reasons.add("마감이 임박해 제출 준비 우선순위가 높습니다.");
        }
        if (keywordMatches > 0) {
            reasons.add("공모전 주제와 기준 데이터의 키워드가 일부 일치합니다.");
        }
        if (reasons.size() < 3) {
            reasons.add("AI 키가 없어 로컬 fallback 점수로 산정했습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fitScore", Math.max(35, Math.min(95, Math.round(score))));
        result.put("reasons", reasons);
        result.put("calculationMethod", "LOCAL_FALLBACK");
        return result;
    }

    private int keywordMatches(String contestText, String basisText) {
        String contest = contestText.toLowerCase(Locale.ROOT);
        String basis = basisText.toLowerCase(Locale.ROOT);
        int count = 0;
        for (String token : contest.split("[\\s,/·]+")) {
            String clean = token.replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (clean.length() >= 2 && basis.contains(clean)) {
                count++;
            }
        }
        return Math.min(count, 6);
    }

    private boolean isFresh(Map<String, Object> existing) {
        Object expiresAt = existing.get("expiresAt");
        return expiresAt instanceof LocalDateTime expires && expires.isAfter(LocalDateTime.now());
    }

    private Map<String, Object> normalizeContestRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        normalizeFilterFields(result);
        String status = text(row.get("status"));
        Number dDay = (Number) row.get("dDay");
        if ("ENDED".equals(status) || (dDay != null && dDay.intValue() < 0)) {
            result.put("badge", "종료됨");
            result.put("badgeReason", "접수 기간 종료");
        } else if (dDay != null && dDay.intValue() <= 7) {
            result.put("badge", "마감 임박");
            result.put("badgeReason", dDay.intValue() + "일 남음");
        } else {
            result.put("badge", null);
            result.put("badgeReason", null);
        }
        return result;
    }

    private Map<String, Object> normalizeFit(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("reasons", reasons(row.get("reasonJson")));
        return result;
    }

    private Map<String, Object> normalizePreparation(Map<String, Object> row) {
        if (row == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("checklistItems", DEFAULT_CHECKLIST);
            empty.put("memo", "");
            empty.put("clickCount", 0);
            return empty;
        }
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("checklistItems", reasons(row.get("checklistJson")));
        return result;
    }

    private List<String> cleanChecklist(List<String> items) {
        if (items == null || items.isEmpty()) {
            return DEFAULT_CHECKLIST;
        }
        return items.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(12)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> reasons(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        try {
            return objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of(String.valueOf(raw));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 변환 중 오류가 발생했습니다.");
        }
    }

    private String normalizeStatus(String status) {
        String value = textOrDefault(status, "OPEN").toUpperCase(Locale.ROOT);
        return List.of("OPEN", "ENDED", "ALL").contains(value) ? value : "OPEN";
    }

    private String normalizeSort(String sort) {
        String value = textOrDefault(sort, "deadline").toLowerCase(Locale.ROOT);
        return List.of("popular", "latest", "deadline").contains(value) ? value : "deadline";
    }

    private List<String> normalizeCodes(List<String> values, Set<String> allowed, String label) {
        if (values == null) return List.of();
        List<String> normalized = values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (!allowed.containsAll(normalized)) throw new SlateException(label + " 필터 값이 올바르지 않습니다.");
        return normalized;
    }

    private RegionFilter normalizeContestRegionFilter(List<String> values) {
        if (values == null) return new RegionFilter(null, List.of());
        List<String> normalized = values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.contains("ALL")) return new RegionFilter(null, List.of());
        if (normalized.size() == 1 && ContestFilterCatalog.LIST_REGION_FILTERS.contains(normalized.get(0))) {
            return new RegionFilter(normalized.get(0), List.of());
        }
        if (!ContestFilterCatalog.REGIONS.containsAll(normalized)) throw new SlateException("지역 필터 값이 올바르지 않습니다.");
        return new RegionFilter(null, normalized);
    }

    private Integer normalizeDeadlineWithinDays(Integer value) {
        if (value == null) return null;
        if (List.of(7, 14, 30).contains(value)) return value;
        throw new SlateException("마감 기간 필터 값이 올바르지 않습니다.");
    }

    private String normalizeSingleCode(String value, Set<String> allowed, String label) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new SlateException(label + " 값이 올바르지 않습니다.");
        return normalized;
    }

    private String codesJson(List<String> values, Set<String> allowed, String label) {
        List<String> normalized = normalizeCodes(values, allowed, label);
        return normalized.isEmpty() ? null : toJson(normalized);
    }

    private void validateAmountRange(Long min, Long max, String label) {
        if ((min != null && min < 0) || (max != null && max < 0)) throw new SlateException(label + " 범위는 0 이상이어야 합니다.");
        if (min != null && max != null && min > max) throw new SlateException(label + " 최소 금액은 최대 금액보다 클 수 없습니다.");
    }

    private void validatePrizeAmounts(Long total, Long first) {
        validateAmountRange(total, total, "총상금");
        validateAmountRange(first, first, "1등 상금");
        if (total != null && first != null && first > total) throw new SlateException("1등 상금은 총상금보다 클 수 없습니다.");
    }

    private void normalizeFilterFields(Map<String, Object> row) {
        row.put("targetCodes", reasons(row.remove("targetCodesJson")));
        row.put("regionCodes", reasons(row.remove("regionCodesJson")));
    }

    private String normalizeExternalImageUrl(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            URI uri = URI.create(value.trim());
            if (!List.of("http", "https").contains(String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT)) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri.toString();
        } catch (Exception ex) {
            throw new SlateException("대표 이미지 URL은 유효한 HTTP 또는 HTTPS 주소여야 합니다.");
        }
    }

    private String normalizeContestType(String contestType) {
        if (!StringUtils.hasText(contestType) || "ALL".equalsIgnoreCase(contestType)) {
            return null;
        }
        String value = contestType.trim().toUpperCase(Locale.ROOT);
        return List.of("INTERNAL", "EXTERNAL").contains(value) ? value : null;
    }

    private String normalizeContestTypeForWrite(String contestType) {
        String value = textOrDefault(contestType, "INTERNAL").toUpperCase(Locale.ROOT);
        if (!List.of("INTERNAL", "EXTERNAL").contains(value)) {
            throw new SlateException("공모전 유형은 INTERNAL 또는 EXTERNAL이어야 합니다.");
        }
        return value;
    }

    private String normalizeRequestStatus(String status) {
        String value = textOrDefault(status, "PENDING").toUpperCase(Locale.ROOT);
        return List.of("PENDING", "APPROVED", "REJECTED", "ALL").contains(value) ? value : "PENDING";
    }

    private String normalizeContestStatusForWrite(String status, boolean admin) {
        String value = textOrDefault(status, "ENDED").toUpperCase(Locale.ROOT);
        List<String> allowed = admin ? List.of("OPEN", "ENDED") : List.of("ENDED");
        if (!allowed.contains(value)) {
            throw new SlateException(admin ? "공모전 상태는 OPEN 또는 ENDED만 가능합니다." : "회사 계정은 공모전 종료만 요청할 수 있습니다.");
        }
        return value;
    }

    private String statusTitle(String status) {
        return "ENDED".equals(status) ? "공모전이 종료되었습니다." : "공모전이 다시 진행 중으로 변경되었습니다.";
    }

    private String normalizeDecision(String decision) {
        String value = textOrDefault(decision, "").toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED").contains(value)) {
            throw new SlateException("decision은 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return value;
    }

    private List<Long> normalizeContestIds(List<Long> contestIds) {
        if (contestIds == null) {
            throw new SlateException("삭제할 공모전을 선택해주세요.");
        }
        List<Long> normalized = contestIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new SlateException("삭제할 공모전을 선택해주세요.");
        }
        if (normalized.size() > 100) {
            throw new SlateException("공모전은 한 번에 100건까지만 삭제할 수 있습니다.");
        }
        return normalized;
    }

    private String normalizeBasisTypeOrNull(String basisType) {
        if (!StringUtils.hasText(basisType)) {
            return null;
        }
        return normalizeBasisType(basisType);
    }

    private String normalizeBasisType(String basisType) {
        String value = textOrDefault(basisType, "PROFILE").toUpperCase(Locale.ROOT);
        if ("USER_PROFILE".equals(value)) {
            value = "PROFILE";
        }
        if (!List.of("PROFILE", "TEAM").contains(value)) {
            throw new SlateException("적합도 기준은 PROFILE 또는 TEAM이어야 합니다.");
        }
        return value;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 500 : limit, 500));
    }

    private int safeAdminLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 30 : limit, 100));
    }

    private LocalDateTime parseRequiredDateTime(String value, String message) {
        LocalDateTime parsed = parseDateTimeOrNull(value, message);
        if (parsed == null) {
            throw new SlateException(message);
        }
        return parsed;
    }

    private LocalDateTime parseDateTimeOrNull(String value, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 16) {
            normalized += ":00";
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            throw new SlateException(message);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = text(value).trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private Map<String, Object> operationPayload(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key1, value1);
        payload.put(key2, value2);
        payload.put(key3, value3);
        return payload;
    }

    private Map<String, Object> auditPayload(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "contestId", "contestType", "title", "status", "deadlineAt", "organizer",
                "requesterCompanyUserId", "sourceRequestId", "saveCount", "prepareCount"
        )) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        return result;
    }

    private Map<String, Object> auditRequestPayload(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "requestId", "requesterUserId", "title", "status", "deadlineAt", "organizer", "approvedContestId"
        )) {
            if (row.containsKey(key)) result.put(key, row.get(key));
        }
        return result;
    }

    private Map<String, Object> normalizeOpenRequest(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.remove("representativeImagePath");
        normalizeFilterFields(result);
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record RegionFilter(String mode, List<String> codes) { }
}
