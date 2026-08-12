package com.slate.boards;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.security.CurrentUser;
import com.slate.teams.TeamMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkFileService {

    private static final long MAX_FILE_BYTES = 300L * 1024 * 1024;
    private static final long USER_QUOTA_BYTES = 1024L * 1024 * 1024;
    private static final long TEAM_QUOTA_BYTES = 2L * 1024 * 1024 * 1024;
    private static final int MAX_DURATION_SECONDS = 180;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("mp4", "webm", "mov");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("video/mp4", "video/webm", "video/quicktime", "application/octet-stream");
    private static final List<String> FILE_STATUSES = List.of("ACTIVE", "HELD", "DELETED");

    private final WorkFileMapper workFileMapper;
    private final TeamMapper teamMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final Path uploadRoot;
    private final String ffprobePath;

    public WorkFileService(
            WorkFileMapper workFileMapper,
            TeamMapper teamMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            @Value("${slate.upload.dir:${SLATE_UPLOAD_DIR:uploads}}") String uploadDir,
            @Value("${slate.upload.ffprobe-path:${SLATE_FFPROBE_PATH:ffprobe}}") String ffprobePath
    ) {
        this.workFileMapper = workFileMapper;
        this.teamMapper = teamMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.ffprobePath = ffprobePath;
    }

    @Transactional
    public Map<String, Object> upload(Long userId, Long teamId, Integer clientDurationSeconds, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SlateException("업로드할 파일을 선택해주세요.");
        }
        validateTeamAccess(userId, teamId);
        validateFileType(file);
        validateSizeAndQuota(userId, teamId, file.getSize());
        Path storedPath = store(file);
        Integer durationSeconds = resolveDuration(storedPath, clientDurationSeconds);
        if (durationSeconds == null) {
            deleteQuietly(storedPath);
            throw new SlateException("영상 길이를 확인할 수 없습니다. ffprobe 설치 또는 브라우저 duration 전달이 필요합니다.");
        }
        if (durationSeconds > MAX_DURATION_SECONDS) {
            deleteQuietly(storedPath);
            throw new SlateException("서버 업로드 영상은 최대 3분까지 등록할 수 있습니다.");
        }
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("uploaderUserId", userId);
        row.put("teamId", teamId);
        row.put("originalName", sanitizeName(file.getOriginalFilename()));
        row.put("storedPath", uploadRoot.relativize(storedPath).toString().replace('\\', '/'));
        row.put("contentType", contentType(file));
        row.put("sizeBytes", file.getSize());
        row.put("durationSeconds", durationSeconds);
        workFileMapper.insertFileMetadata(row);
        Map<String, Object> uploaded = requireFile(longValue(row.get("fileId")));
        auditLogService.recordAudit(userId, "WORK_FILE_UPLOADED", "FILE", longValue(uploaded.get("fileId")), null, auditPayload(uploaded));
        return uploaded;
    }

    public Map<String, Object> myFiles(Long userId, String status, Integer limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("files", workFileMapper.selectFilesByUploader(userId, normalizeStatus(status), safeLimit(limit)));
        result.put("quota", quota(userId, null, 0L));
        return result;
    }

    @Transactional
    public Map<String, Object> deleteOwnFile(Long userId, Long fileId) {
        Map<String, Object> before = requireFile(fileId);
        assertOwner(userId, before);
        String status = Objects.toString(before.get("status"), "");
        if ("HELD".equals(status)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "운영 보관 중인 파일은 직접 삭제할 수 없습니다.");
        }
        if (!"ACTIVE".equals(status)) {
            throw new SlateException("활성 파일만 삭제할 수 있습니다.");
        }
        if (workFileMapper.countFileReferences(fileId) > 0) {
            throw new SlateException("게시글 또는 승인 요청에 연결된 파일은 먼저 작업물 연결을 해제해야 합니다.");
        }
        workFileMapper.softDeleteFile(fileId, "USER_DELETED");
        Map<String, Object> after = requireFile(fileId);
        auditLogService.recordAudit(userId, "WORK_FILE_DELETED", "FILE", fileId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("INFO", "WORK_FILE_DELETED", "사용자가 업로드 파일을 삭제 상태로 전환했습니다.", operationContext(fileId, userId, "USER_DELETED"));
        return after;
    }

    @Transactional
    public Map<String, Object> restoreOwnFile(Long userId, Long fileId) {
        Map<String, Object> before = requireFile(fileId);
        assertOwner(userId, before);
        if (!"DELETED".equals(before.get("status"))) {
            throw new SlateException("삭제 상태 파일만 복구할 수 있습니다.");
        }
        if (!"USER_DELETED".equals(before.get("holdReason"))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "관리자 처리 파일은 직접 복구할 수 없습니다.");
        }
        assertPhysicalFilePresent(before);
        validateRestoreQuota(before);
        workFileMapper.restoreFile(fileId);
        Map<String, Object> after = requireFile(fileId);
        auditLogService.recordAudit(userId, "WORK_FILE_RESTORED", "FILE", fileId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("INFO", "WORK_FILE_RESTORED", "사용자가 삭제 파일을 복구했습니다.", operationContext(fileId, userId, "USER_RESTORE"));
        return after;
    }

    public List<Map<String, Object>> adminFiles(Long adminUserId, String status, String keyword, Long uploaderUserId, Long teamId, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        return workFileMapper.selectFiles(normalizeStatus(status), textOrNull(keyword), uploaderUserId, teamId, safeLimit(limit));
    }

    public Map<String, Object> adminStorageSummary(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", workFileMapper.selectStorageSummary());
        result.put("topUsers", workFileMapper.selectTopUsersByActiveSize(5));
        result.put("topTeams", workFileMapper.selectTopTeamsByActiveSize(5));
        result.put("userQuotaBytes", USER_QUOTA_BYTES);
        result.put("teamQuotaBytes", TEAM_QUOTA_BYTES);
        return result;
    }

    @Transactional
    public Map<String, Object> adminHoldFile(Long adminUserId, Long fileId, String reason) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        Map<String, Object> before = requireFile(fileId);
        if ("HELD".equals(before.get("status"))) {
            throw new SlateException("이미 보관 중인 파일입니다.");
        }
        String cleanReason = reasonOrDefault(reason, "운영 정책 검토로 보관");
        workFileMapper.holdFile(fileId, cleanReason);
        Map<String, Object> after = requireFile(fileId);
        auditLogService.recordAudit(adminUserId, "WORK_FILE_HELD", "FILE", fileId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("WARN", "WORK_FILE_HELD", "관리자가 업로드 파일을 보관 처리했습니다.", operationContext(fileId, adminUserId, cleanReason));
        return after;
    }

    @Transactional
    public Map<String, Object> adminDeleteFile(Long adminUserId, Long fileId, String reason) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        Map<String, Object> before = requireFile(fileId);
        if ("DELETED".equals(before.get("status"))) {
            throw new SlateException("이미 삭제 상태인 파일입니다.");
        }
        String cleanReason = reasonOrDefault(reason, "관리자 삭제");
        workFileMapper.softDeleteFile(fileId, "ADMIN_DELETED: " + cleanReason);
        Map<String, Object> after = requireFile(fileId);
        auditLogService.recordAudit(adminUserId, "WORK_FILE_ADMIN_DELETED", "FILE", fileId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("WARN", "WORK_FILE_ADMIN_DELETED", "관리자가 업로드 파일을 삭제 상태로 전환했습니다.", operationContext(fileId, adminUserId, cleanReason));
        return after;
    }

    @Transactional
    public Map<String, Object> adminRestoreFile(Long adminUserId, Long fileId, String reason) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        Map<String, Object> before = requireFile(fileId);
        if ("ACTIVE".equals(before.get("status"))) {
            throw new SlateException("이미 활성 상태인 파일입니다.");
        }
        assertPhysicalFilePresent(before);
        validateRestoreQuota(before);
        workFileMapper.restoreFile(fileId);
        Map<String, Object> after = requireFile(fileId);
        auditLogService.recordAudit(adminUserId, "WORK_FILE_ADMIN_RESTORED", "FILE", fileId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("INFO", "WORK_FILE_ADMIN_RESTORED", "관리자가 업로드 파일을 활성 상태로 복구했습니다.", operationContext(fileId, adminUserId, reasonOrDefault(reason, "관리자 복구")));
        return after;
    }

    public void assertFileUsable(Long userId, Long teamId, Long fileId) {
        if (fileId == null) {
            return;
        }
        Map<String, Object> file = requireFile(fileId);
        if (!"ACTIVE".equals(file.get("status"))) {
            throw new SlateException("사용할 수 없는 파일입니다.");
        }
        if (!Objects.equals(longValue(file.get("uploaderUserId")), userId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "본인이 업로드한 파일만 작업물에 연결할 수 있습니다.");
        }
        Long fileTeamId = longValue(file.get("teamId"));
        if (!Objects.equals(fileTeamId, teamId)) {
            throw new SlateException("파일의 팀 정보와 작업물 팀 정보가 일치하지 않습니다.");
        }
    }

    public Map<String, Object> file(Long fileId) {
        return requireFile(fileId);
    }

    public ResponseEntity<Resource> stream(Long fileId, CurrentUser currentUser) {
        Map<String, Object> file = requireFile(fileId);
        if (!"ACTIVE".equals(file.get("status"))) {
            throw new SlateException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        assertStreamAllowed(fileId, file, currentUser);
        Path path = resolveStoredPath(Objects.toString(file.get("storedPath"), ""));
        if (!Files.isRegularFile(path)) {
            throw new SlateException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Objects.toString(file.get("contentType"), MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                .contentLength(path.toFile().length())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeName(Objects.toString(file.get("originalName"), "work-video")) + "\"")
                .body(resource);
    }

    private void assertStreamAllowed(Long fileId, Map<String, Object> file, CurrentUser currentUser) {
        Long requesterUserId = currentUser == null ? null : currentUser.userId();
        if (currentUser != null && currentUser.isAdmin()) {
            return;
        }
        if (requesterUserId != null && Objects.equals(requesterUserId, longValue(file.get("uploaderUserId")))) {
            return;
        }
        List<Map<String, Object>> accessRows = workFileMapper.selectStreamAccessRows(fileId);
        if (accessRows.stream().anyMatch(this::isPubliclyStreamable)) {
            return;
        }
        if (requesterUserId == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "파일을 보려면 로그인이 필요합니다.");
        }
        for (Map<String, Object> row : accessRows) {
            if (Objects.equals(requesterUserId, longValue(row.get("workOwnerUserId")))
                    || Objects.equals(requesterUserId, longValue(row.get("postAuthorUserId")))
                    || Objects.equals(requesterUserId, longValue(row.get("requestUserId")))
                    || isActiveTeamMember(requesterUserId, longValue(row.get("fileTeamId")))
                    || isActiveTeamMember(requesterUserId, longValue(row.get("workTeamId")))
                    || isActiveTeamMember(requesterUserId, longValue(row.get("requestTeamId")))) {
                return;
            }
        }
        throw new SlateException(HttpStatus.FORBIDDEN, "파일을 볼 권한이 없습니다.");
    }

    private boolean isPubliclyStreamable(Map<String, Object> row) {
        return "PUBLIC".equals(row.get("postVisibility"))
                && "PUBLIC".equals(row.get("workVisibility"))
                && "PUBLISHED".equals(row.get("postStatus"))
                && "PUBLISHED".equals(row.get("workStatus"))
                && row.get("postDeletedAt") == null;
    }

    private boolean isActiveTeamMember(Long userId, Long teamId) {
        return userId != null && teamId != null && StringUtils.hasText(teamMapper.selectActiveTeamRole(teamId, userId));
    }

    private Path store(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        LocalDate now = LocalDate.now();
        Path dir = uploadRoot.resolve(Path.of("work-items", String.valueOf(now.getYear()), "%02d".formatted(now.getMonthValue()))).normalize();
        if (!dir.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 경로가 올바르지 않습니다.");
        }
        try {
            Files.createDirectories(dir);
            Path destination = dir.resolve(UUID.randomUUID() + "." + extension).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }

    private Integer resolveDuration(Path storedPath, Integer clientDurationSeconds) {
        Integer probed = probeDuration(storedPath);
        if (probed != null) {
            return probed;
        }
        if (clientDurationSeconds != null && clientDurationSeconds > 0) {
            return clientDurationSeconds;
        }
        return null;
    }

    private Integer probeDuration(Path storedPath) {
        try {
            Process process = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    storedPath.toString()
            ).redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            int exit = process.exitValue();
            if (exit != 0 || !StringUtils.hasText(output)) {
                return null;
            }
            return (int) Math.ceil(Double.parseDouble(output.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void validateTeamAccess(Long userId, Long teamId) {
        if (teamId == null) {
            return;
        }
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        if (!StringUtils.hasText(role)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀 작업물 파일은 팀 멤버만 업로드할 수 있습니다.");
        }
    }

    private void validateFileType(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SlateException("서버 업로드 영상은 mp4, webm, mov 파일만 허용합니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType(file))) {
            throw new SlateException("영상 파일 형식이 올바르지 않습니다.");
        }
    }

    private void validateSizeAndQuota(Long userId, Long teamId, long size) {
        if (size > MAX_FILE_BYTES) {
            throw new SlateException("영상 파일은 최대 300MB까지 업로드할 수 있습니다.");
        }
        long userUsed = numberOrZero(workFileMapper.sumActiveSizeByUser(userId));
        if (userUsed + size > USER_QUOTA_BYTES) {
            throw new SlateException("사용자 업로드 용량 1GB를 초과합니다.");
        }
        if (teamId != null) {
            long teamUsed = numberOrZero(workFileMapper.sumActiveSizeByTeam(teamId));
            if (teamUsed + size > TEAM_QUOTA_BYTES) {
                throw new SlateException("팀 업로드 용량 2GB를 초과합니다.");
            }
        }
    }

    private void validateRestoreQuota(Map<String, Object> file) {
        Long userId = longValue(file.get("uploaderUserId"));
        Long teamId = longValue(file.get("teamId"));
        long size = numberOrZero((Number) file.get("sizeBytes"));
        Map<String, Object> quota = quota(userId, teamId, size);
        if ((Boolean) quota.get("userQuotaExceeded")) {
            throw new SlateException("복구하면 사용자 업로드 용량 1GB를 초과합니다.");
        }
        if ((Boolean) quota.get("teamQuotaExceeded")) {
            throw new SlateException("복구하면 팀 업로드 용량 2GB를 초과합니다.");
        }
    }

    private Map<String, Object> quota(Long userId, Long teamId, long additionalBytes) {
        long userUsed = userId == null ? 0L : numberOrZero(workFileMapper.sumActiveSizeByUser(userId));
        long teamUsed = teamId == null ? 0L : numberOrZero(workFileMapper.sumActiveSizeByTeam(teamId));
        Map<String, Object> quota = new LinkedHashMap<>();
        quota.put("activeUserBytes", userUsed);
        quota.put("userQuotaBytes", USER_QUOTA_BYTES);
        quota.put("userRemainingBytes", Math.max(0L, USER_QUOTA_BYTES - userUsed));
        quota.put("activeTeamBytes", teamUsed);
        quota.put("teamQuotaBytes", TEAM_QUOTA_BYTES);
        quota.put("teamRemainingBytes", teamId == null ? null : Math.max(0L, TEAM_QUOTA_BYTES - teamUsed));
        quota.put("additionalBytes", additionalBytes);
        quota.put("userQuotaExceeded", userUsed + additionalBytes > USER_QUOTA_BYTES);
        quota.put("teamQuotaExceeded", teamId != null && teamUsed + additionalBytes > TEAM_QUOTA_BYTES);
        return quota;
    }

    private void assertOwner(Long userId, Map<String, Object> file) {
        if (!Objects.equals(longValue(file.get("uploaderUserId")), userId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "본인이 업로드한 파일만 관리할 수 있습니다.");
        }
    }

    private void assertPhysicalFilePresent(Map<String, Object> file) {
        Path path = resolveStoredPath(Objects.toString(file.get("storedPath"), ""));
        if (!Files.isRegularFile(path)) {
            throw new SlateException(HttpStatus.NOT_FOUND, "저장된 물리 파일을 찾을 수 없어 복구할 수 없습니다.");
        }
    }

    private Map<String, Object> requireFile(Long fileId) {
        Map<String, Object> file = workFileMapper.selectFileById(fileId);
        if (file == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        return file;
    }

    private Path resolveStoredPath(String storedPath) {
        Path path = uploadRoot.resolve(storedPath).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.BAD_REQUEST, "파일 경로가 올바르지 않습니다.");
        }
        return path;
    }

    private String extension(String filename) {
        String name = sanitizeName(filename);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sanitizeName(String filename) {
        String name = StringUtils.hasText(filename) ? filename.trim() : "work-video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String contentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : "application/octet-stream";
    }

    private long numberOrZero(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!FILE_STATUSES.contains(normalized)) {
            throw new SlateException("파일 상태 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 30 : limit, 100));
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String reasonOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> auditPayload(Map<String, Object> file) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("fileId", file.get("fileId"));
        payload.put("teamId", file.get("teamId"));
        payload.put("originalName", file.get("originalName"));
        payload.put("contentType", file.get("contentType"));
        payload.put("sizeBytes", file.get("sizeBytes"));
        payload.put("durationSeconds", file.get("durationSeconds"));
        payload.put("status", file.get("status"));
        payload.put("holdReason", file.get("holdReason"));
        payload.put("deletedAt", file.get("deletedAt"));
        payload.put("physicalDeleteDueAt", file.get("physicalDeleteDueAt"));
        return payload;
    }

    private Map<String, Object> operationContext(Long fileId, Long actorUserId, String reason) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("fileId", fileId);
        context.put("actorUserId", actorUserId);
        context.put("reason", reason);
        return context;
    }
}
