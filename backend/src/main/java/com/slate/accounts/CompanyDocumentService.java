package com.slate.accounts;

import java.io.BufferedInputStream;
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

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyDocumentService {

    private static final long MAX_DOCUMENT_BYTES = 20L * 1024 * 1024;
    private static final List<String> DOCUMENT_TYPES = List.of("BUSINESS_REGISTRATION", "COMPANY_PROFILE", "PORTFOLIO", "OTHER");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "png", "jpg", "jpeg", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/octet-stream"
    );

    private final AccountMapper accountMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final Path uploadRoot;

    public CompanyDocumentService(
            AccountMapper accountMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            @Value("${slate.upload.dir:${SLATE_UPLOAD_DIR:uploads}}") String uploadDir
    ) {
        this.accountMapper = accountMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public Map<String, Object> publicUpload(Long applicationId, String businessRegistrationNo, String documentType, MultipartFile file) {
        Map<String, Object> application = requireApplication(applicationId);
        if (!Objects.equals(normalizeBusinessNo(application.get("businessRegistrationNo")), normalizeBusinessNo(businessRegistrationNo))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "회사 신청 정보가 일치하지 않습니다.");
        }
        return storeDocument(application, normalizeDocumentType(documentType), file, "COMPANY_DOCUMENT_UPLOADED_PUBLIC");
    }

    public List<Map<String, Object>> myDocuments(CurrentUser currentUser) {
        Map<String, Object> application = requireMyCompanyApplication(currentUser);
        return accountMapper.selectCompanyApplicationDocuments(longValue(application.get("companyApplicationId")));
    }

    @Transactional
    public Map<String, Object> uploadMyDocument(CurrentUser currentUser, String documentType, MultipartFile file) {
        Map<String, Object> application = requireMyCompanyApplication(currentUser);
        return storeDocument(application, normalizeDocumentType(documentType), file, "COMPANY_DOCUMENT_UPLOADED");
    }

    @Transactional
    public Map<String, Object> deleteMyDocument(CurrentUser currentUser, Long documentId) {
        Map<String, Object> application = requireMyCompanyApplication(currentUser);
        Map<String, Object> before = requireDocument(documentId);
        if (!Objects.equals(longValue(before.get("companyApplicationId")), longValue(application.get("companyApplicationId")))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "본인 회사 서류만 삭제할 수 있습니다.");
        }
        if (accountMapper.softDeleteCompanyApplicationDocument(documentId) == 0) {
            throw new SlateException("삭제할 수 있는 서류가 아닙니다.");
        }
        Map<String, Object> after = requireDocument(documentId);
        auditLogService.recordAudit(currentUser.userId(), "COMPANY_DOCUMENT_DELETED", "COMPANY_APPLICATION_DOCUMENT", documentId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("INFO", "COMPANY_DOCUMENT_DELETED", "회사 계정이 승인 서류를 삭제했습니다.", operationContext(documentId, currentUser.userId()));
        return after;
    }

    public List<Map<String, Object>> adminDocuments(Long adminUserId, Long applicationId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.COMPANY_APPROVAL);
        requireApplication(applicationId);
        return accountMapper.selectCompanyApplicationDocuments(applicationId);
    }

    public ResponseEntity<Resource> adminDownload(Long adminUserId, Long documentId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.COMPANY_APPROVAL);
        Map<String, Object> document = requireDocument(documentId);
        if (!"ACTIVE".equals(document.get("status"))) {
            throw new SlateException(HttpStatus.NOT_FOUND, "다운로드할 서류를 찾을 수 없습니다.");
        }
        Path path = resolveStoredPath(Objects.toString(document.get("storedPath"), ""));
        if (!Files.isRegularFile(path)) {
            throw new SlateException(HttpStatus.NOT_FOUND, "저장된 서류 파일을 찾을 수 없습니다.");
        }
        auditLogService.recordOperation("INFO", "COMPANY_DOCUMENT_DOWNLOADED", "관리자가 회사 승인 서류를 다운로드했습니다.", operationContext(documentId, adminUserId));
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Objects.toString(document.get("contentType"), MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                .contentLength(path.toFile().length())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(sanitizeName(Objects.toString(document.get("originalName"), "company-document")))
                        .build()
                        .toString())
                .body(resource);
    }

    private Map<String, Object> storeDocument(Map<String, Object> application, String documentType, MultipartFile file, String actionType) {
        validateFile(file);
        Path storedPath = store(file);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("companyApplicationId", application.get("companyApplicationId"));
        document.put("uploaderUserId", application.get("userId"));
        document.put("documentType", documentType);
        document.put("originalName", sanitizeName(file.getOriginalFilename()));
        document.put("storedPath", uploadRoot.relativize(storedPath).toString().replace('\\', '/'));
        document.put("contentType", contentType(file));
        document.put("sizeBytes", file.getSize());
        accountMapper.insertCompanyApplicationDocument(document);
        Map<String, Object> saved = requireDocument(longValue(document.get("documentId")));
        auditLogService.recordAudit(longValue(application.get("userId")), actionType, "COMPANY_APPLICATION_DOCUMENT", longValue(saved.get("documentId")), null, auditPayload(saved));
        auditLogService.recordOperation("INFO", actionType, "회사 승인 서류가 업로드되었습니다.", operationContext(longValue(saved.get("documentId")), longValue(application.get("userId"))));
        return saved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SlateException("업로드할 회사 서류 파일을 선택해주세요.");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new SlateException("회사 서류는 파일당 최대 20MB까지 업로드할 수 있습니다.");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SlateException("회사 서류는 pdf, png, jpg, webp 파일만 허용합니다.");
        }
        String contentType = contentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new SlateException("회사 서류 파일 형식이 올바르지 않습니다.");
        }
        if ("application/octet-stream".equals(contentType) && !looksLikeAllowedFile(file, extension)) {
            throw new SlateException("회사 서류 파일 형식을 확인할 수 없습니다.");
        }
    }

    private boolean looksLikeAllowedFile(MultipartFile file, String extension) {
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(12);
            byte[] header = input.readNBytes(12);
            input.reset();
            if ("pdf".equals(extension)) {
                return header.length >= 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            }
            if ("png".equals(extension)) {
                return header.length >= 4 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            }
            if ("jpg".equals(extension) || "jpeg".equals(extension)) {
                return header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            }
            if ("webp".equals(extension)) {
                return header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Path store(MultipartFile file) {
        LocalDate now = LocalDate.now();
        Path dir = uploadRoot.resolve(Path.of("company-documents", String.valueOf(now.getYear()), "%02d".formatted(now.getMonthValue()))).normalize();
        if (!dir.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 경로가 올바르지 않습니다.");
        }
        try {
            Files.createDirectories(dir);
            Path destination = dir.resolve(UUID.randomUUID() + "." + extension(file.getOriginalFilename())).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "회사 서류 저장 중 오류가 발생했습니다.");
        }
    }

    private Map<String, Object> requireMyCompanyApplication(CurrentUser currentUser) {
        if (currentUser == null || !"COMPANY".equals(currentUser.accountType())) {
            throw new SlateException(HttpStatus.FORBIDDEN, "회사 계정만 회사 서류를 관리할 수 있습니다.");
        }
        Map<String, Object> application = accountMapper.selectCompanyApplicationByUserId(currentUser.userId());
        if (application == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "회사 승인 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    private Map<String, Object> requireApplication(Long applicationId) {
        Map<String, Object> application = accountMapper.selectCompanyApplicationById(applicationId);
        if (application == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "회사 승인 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    private Map<String, Object> requireDocument(Long documentId) {
        Map<String, Object> document = accountMapper.selectCompanyApplicationDocumentById(documentId);
        if (document == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "회사 서류를 찾을 수 없습니다.");
        }
        return document;
    }

    private Path resolveStoredPath(String storedPath) {
        Path path = uploadRoot.resolve(storedPath).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new SlateException(HttpStatus.BAD_REQUEST, "파일 경로가 올바르지 않습니다.");
        }
        return path;
    }

    private String normalizeDocumentType(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "OTHER";
        if (!DOCUMENT_TYPES.contains(normalized)) {
            throw new SlateException("회사 서류 유형 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeBusinessNo(Object value) {
        return Objects.toString(value, "").replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    private String extension(String filename) {
        String name = sanitizeName(filename);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sanitizeName(String filename) {
        String name = StringUtils.hasText(filename) ? filename.trim() : "company-document";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String contentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : "application/octet-stream";
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private Map<String, Object> auditPayload(Map<String, Object> document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", document.get("documentId"));
        payload.put("companyApplicationId", document.get("companyApplicationId"));
        payload.put("documentType", document.get("documentType"));
        payload.put("originalName", document.get("originalName"));
        payload.put("contentType", document.get("contentType"));
        payload.put("sizeBytes", document.get("sizeBytes"));
        payload.put("status", document.get("status"));
        return payload;
    }

    private Map<String, Object> operationContext(Long documentId, Long actorUserId) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("documentId", documentId);
        context.put("actorUserId", actorUserId);
        return context;
    }
}
