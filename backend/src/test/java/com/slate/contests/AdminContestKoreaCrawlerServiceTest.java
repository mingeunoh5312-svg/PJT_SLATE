package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AdminContestKoreaCrawlerServiceTest {

    @Test
    void runRequiresContestManagePermissionAndRecordsOperationLog() {
        Fixture fixture = fixture();
        ContestKoreaCrawlerRunRequest request = new ContestKoreaCrawlerRunRequest(1, 3, true);
        ContestKoreaCrawlerRunResult runResult = result();
        when(fixture.crawlerService.run(request)).thenReturn(runResult);

        ContestKoreaCrawlerRunResult result = fixture.service.run(99L, request);

        assertThat(result).isSameAs(runResult);
        verify(fixture.adminPermissionService).require(99L, AdminPermissionCatalog.CONTEST_MANAGE);
        verify(fixture.crawlerService).run(request);
        verify(fixture.auditLogService).recordOperation(
                eq("INFO"),
                eq("CONTESTKOREA_CRAWLER_RUN"),
                contains("콘테스트코리아"),
                argThat(value -> {
                    Map<?, ?> context = (Map<?, ?>) value;
                    return Long.valueOf(99L).equals(context.get("adminUserId"))
                            && Boolean.TRUE.equals(context.get("dryRun"))
                            && Integer.valueOf(2).equals(context.get("processedItems"))
                            && Integer.valueOf(1).equals(context.get("insertedCount"))
                            && Integer.valueOf(1).equals(context.get("failedCount"));
                })
        );
    }

    @Test
    void permissionFailureDoesNotRunCrawlerOrWriteOperationLog() {
        Fixture fixture = fixture();
        doThrow(new SlateException(HttpStatus.FORBIDDEN, "no permission"))
                .when(fixture.adminPermissionService)
                .require(99L, AdminPermissionCatalog.CONTEST_MANAGE);

        assertThatThrownBy(() -> fixture.service.run(99L, new ContestKoreaCrawlerRunRequest(1, 1, false)))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("no permission");

        verifyNoInteractions(fixture.crawlerService);
        verify(fixture.auditLogService, never()).recordOperation(eq("INFO"), eq("CONTESTKOREA_CRAWLER_RUN"), contains("콘테스트코리아"), org.mockito.ArgumentMatchers.any());
    }

    private Fixture fixture() {
        ContestKoreaCrawlerService crawlerService = mock(ContestKoreaCrawlerService.class);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        return new Fixture(
                new AdminContestKoreaCrawlerService(crawlerService, adminPermissionService, auditLogService),
                crawlerService,
                adminPermissionService,
                auditLogService
        );
    }

    private ContestKoreaCrawlerRunResult result() {
        return new ContestKoreaCrawlerRunResult(
                true,
                true,
                1,
                3,
                1,
                3,
                3,
                2,
                1,
                0,
                1,
                1,
                0,
                LocalDateTime.of(2026, 6, 23, 11, 0),
                LocalDateTime.of(2026, 6, 23, 11, 1),
                List.of()
        );
    }

    private record Fixture(
            AdminContestKoreaCrawlerService service,
            ContestKoreaCrawlerService crawlerService,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
    }
}
