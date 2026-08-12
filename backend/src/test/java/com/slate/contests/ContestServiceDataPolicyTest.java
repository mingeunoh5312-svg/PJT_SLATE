package com.slate.contests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.media.MediaImageService;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.Test;

class ContestServiceDataPolicyTest {

    @Test
    void emptyPublicListRemainsEmptyAndUsesDeadlineContract() {
        Fixture fixture = fixture();
        ContestSearchCriteria expected = new ContestSearchCriteria(
                "OPEN", "deadline", null, null,
                List.of(), List.of(), List.of(),
                null, null, null, null, 500
        );
        when(fixture.mapper.selectContests(expected, null)).thenReturn(List.of());

        assertThat(fixture.service.contests(
                "OPEN", "deadline", null, null,
                null, null, null,
                null, null, null, null, 500, null
        )).isEmpty();

        verify(fixture.mapper).selectContests(expected, null);
    }

    @Test
    void requestResponsesDoNotExposeInternalStoredPath() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectOpenRequestsByRequester(12L)).thenReturn(List.of(openRequest("images/contest_request/private.jpg")));
        CurrentUser company = new CurrentUser(12L, "company@slate.test", "company", "COMPANY", List.of());

        assertThat(fixture.service.myOpenRequests(company).get(0))
                .doesNotContainKey("representativeImagePath")
                .containsEntry("requestImageUrl", null);
    }

    @Test
    void structuredFiltersAreNormalizedWithoutInventingContestData() {
        Fixture fixture = fixture();
        ContestSearchCriteria expected = new ContestSearchCriteria(
                "OPEN", "deadline", null, "영화",
                List.of("UNIVERSITY"), List.of("SEOUL"), List.of("GOVERNMENT_PUBLIC"),
                10_000_000L, 30_000_000L, 1_000_000L, 3_000_000L, 500
        );
        when(fixture.mapper.selectContests(expected, 1L)).thenReturn(List.of());

        assertThat(fixture.service.contests(
                "OPEN", "deadline", null, "영화",
                List.of("university"), List.of("seoul"), List.of("government_public"),
                10_000_000L, 30_000_000L, 1_000_000L, 3_000_000L, 500, 1L
        )).isEmpty();

        verify(fixture.mapper).selectContests(expected, 1L);
    }

    @Test
    void invalidFilterCodeAndPrizeRangeAreRejectedBeforeQuery() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.contests(
                "OPEN", "deadline", null, null,
                List.of("INVENTED"), null, null,
                null, null, null, null, 500, null
        )).isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> fixture.service.contests(
                "OPEN", "deadline", null, null,
                null, null, null,
                30_000_000L, 10_000_000L, null, null, 500, null
        )).isInstanceOf(SlateException.class);

        verify(fixture.mapper, never()).selectContests(any(), any());
    }

    @Test
    void openRequestRejectsGuestAndRegularUserBeforeDatabaseMutation() {
        Fixture fixture = fixture();
        CurrentUser regularUser = new CurrentUser(2L, "user@slate.test", "user", "USER", List.of());

        assertThatThrownBy(() -> fixture.service.createOpenRequest(null, null)).isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> fixture.service.createOpenRequest(regularUser, null)).isInstanceOf(SlateException.class);

        verify(fixture.mapper, never()).insertContestOpenRequest(any());
    }

    @Test
    void approvalTransfersRequestImageReferenceWithoutDeletingFile() {
        Fixture fixture = fixture();
        Map<String, Object> request = openRequest("images/contest_request/request.jpg");
        when(fixture.mapper.selectContestOpenRequestById(11L)).thenReturn(request);
        doAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("contestId", 31L);
            return 1;
        }).when(fixture.mapper).insertContest(any());

        fixture.service.decideOpenRequest(99L, 11L,
                new AdminContestController.ContestRequestDecisionRequest("APPROVED", "ok"));

        verify(fixture.mapper).clearContestOpenRequestImagePath(11L);
        verify(fixture.mapper).updateContestOpenRequestDecision(11L, "APPROVED", "ok", 99L, 31L);
        verify(fixture.media, never()).deleteStoredAfterCommit(any());
    }

    @Test
    void rejectionClearsReferenceAndSchedulesStoredFileDeletion() {
        Fixture fixture = fixture();
        String path = "images/contest_request/request.jpg";
        Map<String, Object> request = openRequest(path);
        when(fixture.mapper.selectContestOpenRequestById(11L)).thenReturn(request);

        fixture.service.decideOpenRequest(99L, 11L,
                new AdminContestController.ContestRequestDecisionRequest("REJECTED", "invalid"));

        verify(fixture.mapper).clearContestOpenRequestImagePath(11L);
        verify(fixture.media).deleteStoredAfterCommit(path);
        verify(fixture.mapper).updateContestOpenRequestDecision(11L, "REJECTED", "invalid", 99L, null);
    }

    @Test
    void adminDeleteSelectedContestsRemovesDependentRowsAndSchedulesImageDeletion() {
        Fixture fixture = fixture();
        Map<String, Object> contest = contest(31L, "images/contest/poster.jpg");
        when(fixture.mapper.selectContestById(31L, null, null, null)).thenReturn(contest);
        when(fixture.mapper.deleteContestById(31L)).thenReturn(1);

        Map<String, Object> result = fixture.service.adminDeleteContests(
                99L,
                new AdminContestController.ContestDeleteRequest(List.of(31L, 31L), "cleanup")
        );

        assertThat(result)
                .containsEntry("requestedCount", 1)
                .containsEntry("deletedCount", 1);
        verify(fixture.mapper).clearContestOpenRequestApprovedContest(31L);
        verify(fixture.mapper).deleteContestSaves(31L);
        verify(fixture.mapper).deleteContestFitCaches(31L);
        verify(fixture.mapper).deleteContestPreparations(31L);
        verify(fixture.mapper).deleteContestById(31L);
        verify(fixture.media).deleteStoredAfterCommit("images/contest/poster.jpg");
    }

    private Map<String, Object> openRequest(String imagePath) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("requestId", 11L);
        request.put("requesterUserId", 12L);
        request.put("status", "PENDING");
        request.put("contestType", "INTERNAL");
        request.put("title", "actual contest");
        request.put("summary", "summary");
        request.put("organizer", "company");
        request.put("submissionEmail", "contest@slate.test");
        request.put("deadlineAt", "2027-01-01T00:00:00");
        request.put("representativeImagePath", imagePath);
        request.put("requestImageUrl", null);
        return request;
    }

    private Map<String, Object> contest(Long contestId, String imagePath) {
        Map<String, Object> contest = new LinkedHashMap<>();
        contest.put("contestId", contestId);
        contest.put("contestType", "EXTERNAL");
        contest.put("title", "영상 공모전");
        contest.put("summary", "summary");
        contest.put("status", "OPEN");
        contest.put("organizer", "contest korea");
        contest.put("deadlineAt", "2027-01-01T00:00:00");
        contest.put("representativeImagePath", imagePath);
        return contest;
    }

    private Fixture fixture() {
        ContestMapper mapper = mock(ContestMapper.class);
        AuditLogService audit = mock(AuditLogService.class);
        AdminPermissionService permission = mock(AdminPermissionService.class);
        NotificationService notification = mock(NotificationService.class);
        MediaImageService media = mock(MediaImageService.class);
        return new Fixture(mapper, media, new ContestService(mapper, audit, permission, notification, media));
    }

    private record Fixture(ContestMapper mapper, MediaImageService media, ContestService service) { }
}
