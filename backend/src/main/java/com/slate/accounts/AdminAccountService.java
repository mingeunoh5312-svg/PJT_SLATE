package com.slate.accounts;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.accounts.AdminAccountController.DecisionRequest;
import com.slate.common.SlateException;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    private final AccountMapper accountMapper;
    private final NotificationService notificationService;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;

    public AdminAccountService(
            AccountMapper accountMapper,
            NotificationService notificationService,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
        this.accountMapper = accountMapper;
        this.notificationService = notificationService;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> companyApplications(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.COMPANY_APPROVAL);
        return accountMapper.selectCompanyApplications();
    }

    @Transactional
    public Map<String, Object> decide(Long adminUserId, Long applicationId, DecisionRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.COMPANY_APPROVAL);
        Map<String, Object> application = accountMapper.selectCompanyApplicationById(applicationId);
        if (application == null) {
            throw new SlateException("회사 승인 신청을 찾을 수 없습니다.");
        }
        String decision = Objects.toString(request.decision(), "").trim().toUpperCase();
        String accountStatus = switch (decision) {
            case "APPROVED" -> "ACTIVE";
            case "REJECTED" -> "PENDING_APPROVAL";
            default -> throw new SlateException("decision은 APPROVED 또는 REJECTED만 가능합니다.");
        };
        accountMapper.updateCompanyApplicationDecision(applicationId, decision, request.reason(), adminUserId);
        accountMapper.updateAccountStatus(((Number) application.get("userId")).longValue(), accountStatus);
        Long companyUserId = ((Number) application.get("userId")).longValue();
        String title = "APPROVED".equals(decision) ? "회사 계정이 승인되었습니다." : "회사 계정 신청이 거절되었습니다.";
        String body = "APPROVED".equals(decision)
                ? "이제 회사 계정으로 Slate 기능을 사용할 수 있습니다."
                : "신청 보완이 필요합니다. 사유: " + Objects.toString(request.reason(), "보완 필요");
        notificationService.send(companyUserId, adminUserId, "ADMIN", title, body, "COMPANY_APPLICATION", applicationId);
        Map<String, Object> decided = accountMapper.selectCompanyApplicationById(applicationId);
        auditLogService.recordAudit(
                adminUserId,
                "COMPANY_APPLICATION_" + decision,
                "COMPANY_APPLICATION",
                applicationId,
                application,
                decided
        );
        auditLogService.recordOperation(
                "INFO",
                "COMPANY_APPLICATION_DECISION",
                "회사 계정 검토 결과가 저장되었습니다.",
                Map.of("applicationId", applicationId, "decision", decision, "companyUserId", companyUserId)
        );
        return decided;
    }
}
