package com.slate.accounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.accounts.AuthController.AccountUpdateRequest;
import com.slate.accounts.AuthController.AccountWithdrawalRequest;
import com.slate.accounts.AuthController.CompanyApplicationRequest;
import com.slate.accounts.AuthController.LoginRequest;
import com.slate.accounts.AuthController.RegisterRequest;
import com.slate.common.SlateException;
import com.slate.moderation.ModerationService;
import com.slate.operations.AuditLogService;
import com.slate.security.CurrentUser;
import com.slate.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModerationService moderationService;
    private final AuditLogService auditLogService;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, JwtService jwtService, ModerationService moderationService, AuditLogService auditLogService) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.moderationService = moderationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String loginId = normalizeLoginId(request.loginId());
        if (accountMapper.selectAccountByLoginId(loginId) != null) {
            throw new SlateException("이미 사용 중인 아이디입니다.");
        }
        if (accountMapper.selectAccountByEmail(request.email()) != null) {
            throw new SlateException("이미 가입된 이메일입니다.");
        }
        String accountType = normalizeAccountType(request.accountType());
        if ("COMPANY".equals(accountType) && request.company() == null) {
            throw new SlateException("회사 계정은 회사 승인 신청 정보가 필요합니다.");
        }
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("loginId", loginId);
        account.put("email", request.email());
        account.put("passwordHash", passwordEncoder.encode(request.password()));
        account.put("nickname", request.nickname());
        account.put("phone", request.company() == null ? null : request.company().managerPhone());
        account.put("accountType", accountType);
        account.put("accountStatus", "COMPANY".equals(accountType) ? "PENDING_APPROVAL" : "ACTIVE");
        accountMapper.insertAccount(account);
        if ("COMPANY".equals(accountType)) {
            insertCompanyApplication(((Number) account.get("userId")).longValue(), request.company());
        }
        Map<String, Object> created = me(((Number) account.get("userId")).longValue());
        created.put("approvalMessage", "COMPANY".equals(accountType) ? "계정 승인 검토 중입니다." : null);
        return created;
    }

    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        String loginId = normalizeLoginId(request.loginId());
        Map<String, Object> account = accountMapper.selectAccountByLoginId(loginId);
        if (account == null || !passwordEncoder.matches(request.password(), Objects.toString(account.get("passwordHash"), ""))) {
            recordLoginOperation("WARN", "AUTH_LOGIN_FAILED", "로그인 인증 정보가 일치하지 않습니다.", loginId, null, "BAD_CREDENTIALS");
            throw new SlateException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        Long userId = ((Number) account.get("userId")).longValue();
        moderationService.refreshExpiredSanctions(userId);
        account = accountMapper.selectAccountById(userId);
        String status = Objects.toString(account.get("accountStatus"), "");
        if ("PENDING_APPROVAL".equals(status)) {
            recordLoginOperation("WARN", "AUTH_LOGIN_BLOCKED", "승인 대기 회사 계정 로그인이 차단되었습니다.", loginId, userId, status);
            throw new SlateException(HttpStatus.FORBIDDEN, "계정 승인 검토 중입니다.");
        }
        if ("TEMP_SUSPENDED".equals(status)) {
            recordLoginOperation("WARN", "AUTH_LOGIN_BLOCKED", "임시 정지 계정 로그인이 차단되었습니다.", loginId, userId, status);
            throw new SlateException(HttpStatus.FORBIDDEN, "임시 정지된 계정입니다.");
        }
        if ("PERM_SUSPENDED".equals(status)) {
            recordLoginOperation("WARN", "AUTH_LOGIN_BLOCKED", "영구 정지 계정 로그인이 차단되었습니다.", loginId, userId, status);
            throw new SlateException(HttpStatus.FORBIDDEN, "영구 정지된 계정입니다.");
        }
        if ("WITHDRAWN".equals(status)) {
            recordLoginOperation("WARN", "AUTH_LOGIN_BLOCKED", "탈퇴 계정 로그인이 차단되었습니다.", loginId, userId, status);
            throw new SlateException(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다.");
        }
        if (!"ACTIVE".equals(status)) {
            recordLoginOperation("WARN", "AUTH_LOGIN_BLOCKED", "비활성 계정 로그인이 차단되었습니다.", loginId, userId, status);
            throw new SlateException(HttpStatus.FORBIDDEN, "사용할 수 없는 계정 상태입니다.");
        }
        accountMapper.updateLastLoginAt(userId);
        CurrentUser user = toCurrentUser(account);
        Map<String, Object> response = publicAccount(account);
        response.put("accessToken", jwtService.issue(user));
        response.put("tokenType", "Bearer");
        recordLoginOperation("INFO", "AUTH_LOGIN_SUCCEEDED", "로그인에 성공했습니다.", loginId, userId, status);
        return response;
    }

    public Map<String, Object> me(Long userId) {
        Map<String, Object> account = accountMapper.selectAccountById(userId);
        if (account == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "인증 사용자를 찾을 수 없습니다.");
        }
        return publicAccount(account);
    }

    @Transactional
    public Map<String, Object> updateMe(Long userId, AccountUpdateRequest request) {
        Map<String, Object> before = requireActiveAccount(userId);
        String nickname = cleanNickname(request.nickname());
        String email = cleanEmail(request.email());
        boolean emailChanged = !email.equalsIgnoreCase(Objects.toString(before.get("email"), ""));
        boolean passwordChanged = StringUtils.hasText(request.newPassword());
        if ((emailChanged || passwordChanged) && !passwordEncoder.matches(Objects.toString(request.currentPassword(), ""), Objects.toString(before.get("passwordHash"), ""))) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (emailChanged) {
            Map<String, Object> emailOwner = accountMapper.selectAccountByEmail(email);
            if (emailOwner != null && !Objects.equals(longValue(emailOwner.get("userId")), userId)) {
                throw new SlateException("이미 가입된 이메일입니다.");
            }
        }
        String passwordHash = passwordChanged ? passwordEncoder.encode(request.newPassword()) : null;
        if (accountMapper.updateCurrentAccount(userId, nickname, email, passwordHash) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }
        Map<String, Object> after = accountMapper.selectAccountById(userId);
        Map<String, Object> response = publicAccount(after);
        response.put("accessToken", jwtService.issue(toCurrentUser(after)));
        response.put("tokenType", "Bearer");
        auditLogService.recordAudit(userId, "USER_ACCOUNT_SELF_UPDATED", "USER_ACCOUNT", userId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("INFO", "USER_ACCOUNT_SELF_UPDATED", "사용자가 계정 정보를 수정했습니다.", Map.of("userId", userId));
        return response;
    }

    @Transactional
    public Map<String, Object> withdrawMe(Long userId, AccountWithdrawalRequest request) {
        Map<String, Object> before = requireActiveAccount(userId);
        if (!passwordEncoder.matches(Objects.toString(request.currentPassword(), ""), Objects.toString(before.get("passwordHash"), ""))) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }
        if ("ADMIN".equals(before.get("accountType"))) {
            throw new SlateException("관리자 계정은 관리자 권한 관리에서 처리해주세요.");
        }
        if (accountMapper.withdrawCurrentAccount(userId) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }
        Map<String, Object> after = accountMapper.selectAccountById(userId);
        auditLogService.recordAudit(userId, "USER_ACCOUNT_WITHDRAWN", "USER_ACCOUNT", userId, auditPayload(before), auditPayload(after));
        auditLogService.recordOperation("WARN", "USER_ACCOUNT_WITHDRAWN", "사용자가 회원 탈퇴를 완료했습니다.", Map.of("userId", userId));
        return Map.of("withdrawn", true);
    }

    private void insertCompanyApplication(Long userId, CompanyApplicationRequest company) {
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("userId", userId);
        application.put("companyName", company.companyName());
        application.put("businessRegistrationNo", company.businessRegistrationNo());
        application.put("managerName", company.managerName());
        application.put("managerPhone", company.managerPhone());
        application.put("companyIntro", company.companyIntro());
        application.put("publicDataCompanyName", company.publicDataCompanyName());
        accountMapper.insertCompanyApplication(application);
    }

    private String normalizeAccountType(String accountType) {
        String value = Objects.toString(accountType, "USER").trim().toUpperCase();
        if (!List.of("USER", "COMPANY").contains(value)) {
            throw new SlateException("가입 가능한 계정 유형은 USER 또는 COMPANY입니다.");
        }
        return value;
    }

    private String normalizeLoginId(String loginId) {
        String value = Objects.toString(loginId, "").trim();
        if (!value.matches("^[a-zA-Z0-9._-]{4,50}$")) {
            throw new SlateException("아이디는 영문, 숫자, 점, 밑줄, 하이픈으로 4~50자 입력해주세요.");
        }
        return value;
    }

    private Map<String, Object> requireActiveAccount(Long userId) {
        Map<String, Object> account = accountMapper.selectAccountById(userId);
        if (account == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "인증 사용자를 찾을 수 없습니다.");
        }
        if (!"ACTIVE".equals(account.get("accountStatus"))) {
            throw new SlateException(HttpStatus.FORBIDDEN, "활성 계정만 변경할 수 있습니다.");
        }
        return account;
    }

    private String cleanNickname(String nickname) {
        String value = Objects.toString(nickname, "").trim();
        if (!StringUtils.hasText(value)) {
            throw new SlateException("닉네임을 입력해주세요.");
        }
        if (value.length() > 50) {
            throw new SlateException("닉네임은 50자 이하로 입력해주세요.");
        }
        return value;
    }

    private String cleanEmail(String email) {
        String value = Objects.toString(email, "").trim();
        if (!StringUtils.hasText(value)) {
            throw new SlateException("이메일을 입력해주세요.");
        }
        if (value.length() > 255) {
            throw new SlateException("이메일은 255자 이하로 입력해주세요.");
        }
        return value;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> auditPayload(Map<String, Object> account) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", account.get("userId"));
        payload.put("loginId", account.get("loginId"));
        payload.put("email", account.get("email"));
        payload.put("nickname", account.get("nickname"));
        payload.put("accountType", account.get("accountType"));
        payload.put("accountStatus", account.get("accountStatus"));
        payload.put("deactivatedAt", account.get("deactivatedAt"));
        return payload;
    }

    private CurrentUser toCurrentUser(Map<String, Object> account) {
        String type = Objects.toString(account.get("accountType"), "USER");
        return new CurrentUser(
                ((Number) account.get("userId")).longValue(),
                Objects.toString(account.get("email"), ""),
                Objects.toString(account.get("nickname"), ""),
                type,
                authorities(type)
        );
    }

    private List<String> authorities(String accountType) {
        if ("ADMIN".equals(accountType)) {
            return List.of("ROLE_ADMIN", "ROLE_USER");
        }
        if ("COMPANY".equals(accountType)) {
            return List.of("ROLE_COMPANY", "ROLE_USER");
        }
        return List.of("ROLE_USER");
    }

    private void recordLoginOperation(String level, String eventCode, String message, String loginId, Long userId, String reason) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("loginIdHash", auditLogService.fingerprint(loginId));
        if (userId != null) {
            context.put("userId", userId);
        }
        context.put("reason", reason);
        auditLogService.recordOperation(level, eventCode, message, context);
    }

    private Map<String, Object> publicAccount(Map<String, Object> account) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", account.get("userId"));
        result.put("loginId", account.get("loginId"));
        result.put("email", account.get("email"));
        result.put("nickname", account.get("nickname"));
        result.put("phone", account.get("phone"));
        result.put("accountType", account.get("accountType"));
        result.put("accountStatus", account.get("accountStatus"));
        result.put("lastLoginAt", account.get("lastLoginAt"));
        result.put("createdAt", account.get("createdAt"));
        if (StringUtils.hasText(Objects.toString(account.get("companyApplicationStatus"), ""))) {
            result.put("companyApplicationId", account.get("companyApplicationId"));
            result.put("companyApplicationStatus", account.get("companyApplicationStatus"));
            result.put("companyName", account.get("companyName"));
            result.put("reviewReason", account.get("reviewReason"));
        }
        return result;
    }
}
