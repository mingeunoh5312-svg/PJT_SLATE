package com.slate.contests;

import java.util.LinkedHashMap;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.operations.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class AdminContestKoreaCrawlerService {

    private final ContestKoreaCrawlerService crawlerService;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;

    public AdminContestKoreaCrawlerService(
            ContestKoreaCrawlerService crawlerService,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
        this.crawlerService = crawlerService;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
    }

    public ContestKoreaCrawlerRunResult run(Long adminUserId, ContestKoreaCrawlerRunRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTEST_MANAGE);
        ContestKoreaCrawlerRunResult result = crawlerService.run(request);
        auditLogService.recordOperation(
                "INFO",
                "CONTESTKOREA_CRAWLER_RUN",
                "관리자가 콘테스트코리아 크롤러를 실행했습니다.",
                operationContext(adminUserId, result)
        );
        return result;
    }

    private Map<String, Object> operationContext(Long adminUserId, ContestKoreaCrawlerRunResult result) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("adminUserId", adminUserId);
        context.put("dryRun", result.dryRun());
        context.put("requestedMaxPages", result.requestedMaxPages());
        context.put("requestedMaxItems", result.requestedMaxItems());
        context.put("fetchedPages", result.fetchedPages());
        context.put("discoveredItems", result.discoveredItems());
        context.put("deduplicatedItems", result.deduplicatedItems());
        context.put("processedItems", result.processedItems());
        context.put("insertedCount", result.insertedCount());
        context.put("updatedCount", result.updatedCount());
        context.put("skippedCount", result.skippedCount());
        context.put("failedCount", result.failedCount());
        context.put("posterStoredCount", result.posterStoredCount());
        return context;
    }
}
