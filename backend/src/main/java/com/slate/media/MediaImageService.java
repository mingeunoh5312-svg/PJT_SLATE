package com.slate.media;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaImageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("PROFILE", "TEAM", "WORK", "PORTFOLIO", "CONTEST", "CONTEST_REQUEST");
    private static final Map<String, String> EXTENSION_TYPES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    private final MediaImageMapper mapper;
    private final AuditLogService auditLogService;
    private final Path uploadRoot;

    public MediaImageService(MediaImageMapper mapper, AuditLogService auditLogService,
            @Value("${slate.upload.dir:${SLATE_UPLOAD_DIR:uploads}}") String uploadDir) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public Map<String, Object> upload(Long userId, String rawType, Long entityId, MultipartFile file) {
        String type = type(rawType);
        Map<String, Object> target = requireTarget(type, entityId);
        requireOwner(userId, target);
        String extension = validate(file);
        Path stored = store(type, extension, file);
        String relative = relative(stored);
        String previous = string(target.get("storedPath"));
        try {
            if (mapper.updatePath(type, entityId, relative) == 0) throw new SlateException("이미지 대상을 수정하지 못했습니다.");
        } catch (RuntimeException ex) {
            deleteQuietly(stored);
            throw ex;
        }
        scheduleReplacementCleanup(stored, previous);
        auditLogService.recordAudit(userId, type + "_IMAGE_UPDATED", type, entityId, Map.of("hadImage", previous != null), Map.of("hasImage", true));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageUrl", imageUrl(type, entityId));
        return result;
    }

    @Transactional
    public Map<String, Object> delete(Long userId, String rawType, Long entityId) {
        String type = type(rawType);
        Map<String, Object> target = requireTarget(type, entityId);
        requireOwner(userId, target);
        String previous = string(target.get("storedPath"));
        if (mapper.updatePath(type, entityId, null) == 0) throw new SlateException("이미지 대상을 수정하지 못했습니다.");
        scheduleDeleteAfterCommit(previous);
        auditLogService.recordAudit(userId, type + "_IMAGE_DELETED", type, entityId, Map.of("hadImage", previous != null), Map.of("hasImage", false));
        return Map.of("deleted", previous != null);
    }

    public ResponseEntity<Resource> stream(Long requesterUserId, String rawType, Long entityId) {
        String type = type(rawType);
        Map<String, Object> target = requireTarget(type, entityId);
        Long ownerUserId = longValue(target.get("ownerUserId"));
        boolean owner = requesterUserId != null && requesterUserId.equals(ownerUserId);
        if (!owner && (!"PUBLIC".equals(target.get("visibility")) || "DELETED".equals(target.get("status")))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "이미지 조회 권한이 없습니다.");
        }
        String storedPath = string(target.get("storedPath"));
        if (storedPath == null) throw new SlateException(HttpStatus.NOT_FOUND, "등록된 이미지가 없습니다.");
        Path path = resolve(storedPath);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) throw new SlateException(HttpStatus.NOT_FOUND, "이미지 파일을 찾을 수 없습니다.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType(path)))
                .body(resource);
    }

    public void deleteStoredAfterCommit(String storedPath) {
        scheduleDeleteAfterCommit(string(storedPath));
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new SlateException("업로드할 이미지를 선택해주세요.");
        if (file.getSize() > MAX_BYTES) throw new SlateException("이미지는 최대 5MB까지 업로드할 수 있습니다.");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expected = EXTENSION_TYPES.get(extension);
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (expected == null || !expected.equals(declared)) throw new SlateException("JPEG, PNG, WebP 이미지만 업로드할 수 있습니다.");
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!matchesSignature(expected, header)) throw new SlateException("이미지 파일 내용이 확장자와 일치하지 않습니다.");
        } catch (SlateException ex) { throw ex; }
        catch (Exception ex) { throw new SlateException("이미지 파일을 확인하지 못했습니다."); }
        return extension.equals("jpeg") ? "jpg" : extension;
    }

    private boolean matchesSignature(String type, byte[] h) {
        if ("image/jpeg".equals(type)) return h.length >= 3 && (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255;
        if ("image/png".equals(type)) return h.length >= 8 && (h[0] & 255) == 137 && h[1] == 80 && h[2] == 78 && h[3] == 71 && h[4] == 13 && h[5] == 10 && h[6] == 26 && h[7] == 10;
        return h.length >= 12 && h[0] == 82 && h[1] == 73 && h[2] == 70 && h[3] == 70 && h[8] == 87 && h[9] == 69 && h[10] == 66 && h[11] == 80;
    }

    private Path store(String type, String extension, MultipartFile file) {
        LocalDate now = LocalDate.now();
        Path dir = uploadRoot.resolve(Path.of("images", type.toLowerCase(Locale.ROOT), String.valueOf(now.getYear()), "%02d".formatted(now.getMonthValue()))).normalize();
        if (!dir.startsWith(uploadRoot)) throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 경로가 올바르지 않습니다.");
        try {
            Files.createDirectories(dir);
            Path path = dir.resolve(UUID.randomUUID() + "." + extension).normalize();
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (Exception ex) { throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 중 오류가 발생했습니다."); }
    }

    private Map<String, Object> requireTarget(String type, Long id) {
        Map<String, Object> target = mapper.selectTarget(type, id);
        if (target == null) throw new SlateException(HttpStatus.NOT_FOUND, "이미지 대상을 찾을 수 없습니다.");
        return target;
    }
    private void requireOwner(Long userId, Map<String, Object> target) { if (!userId.equals(longValue(target.get("ownerUserId")))) throw new SlateException(HttpStatus.FORBIDDEN, "이미지 수정 권한이 없습니다."); }
    private String type(String value) { String type = value == null ? "" : value.toUpperCase(Locale.ROOT); if (!TYPES.contains(type)) throw new SlateException("지원하지 않는 이미지 대상입니다."); return type; }
    private String imageUrl(String type, Long id) { return "/api/media/images/" + type.toLowerCase(Locale.ROOT) + "/" + id; }
    private String relative(Path path) { return uploadRoot.relativize(path).toString().replace('\\', '/'); }
    private Path resolve(String storedPath) { Path path = uploadRoot.resolve(storedPath).normalize(); if (!path.startsWith(uploadRoot)) throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 경로가 올바르지 않습니다."); return path; }
    private void deleteRelativeQuietly(String value) { if (value != null) deleteQuietly(resolve(value)); }
    private void scheduleReplacementCleanup(Path stored, String previous) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteRelativeQuietly(previous);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteRelativeQuietly(previous); }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteQuietly(stored);
            }
        });
    }
    private void scheduleDeleteAfterCommit(String previous) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteRelativeQuietly(previous);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteRelativeQuietly(previous); }
        });
    }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (Exception ignored) { } }
    private String string(Object value) { return value == null || !StringUtils.hasText(String.valueOf(value)) ? null : String.valueOf(value); }
    private Long longValue(Object value) { return value == null ? null : value instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(value)); }
    private String contentType(Path path) { String name = path.getFileName().toString().toLowerCase(Locale.ROOT); return name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg"; }
}
