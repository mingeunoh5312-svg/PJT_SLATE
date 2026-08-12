package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.slate.operations.AuditLogService;
import com.slate.profiles.ProfileController.PortfolioItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortfolioVerificationServiceTest {

    private ProfileMapper profileMapper;
    private KobisClient kobisClient;
    private PortfolioVerificationService service;

    @BeforeEach
    void setUp() {
        profileMapper = mock(ProfileMapper.class);
        kobisClient = mock(KobisClient.class);
        service = new PortfolioVerificationService(
                profileMapper,
                kobisClient,
                new KobisRoleMatcher(),
                mock(AuditLogService.class)
        );
        when(kobisClient.hasApiKey()).thenReturn(true);
    }

    @Test
    void storesProviderCreditWhenNameAndRoleMatch() {
        when(kobisClient.movieDetail("movie-1")).thenReturn(Optional.of(detail("김크레딧", "촬영")));

        service.verifyAfterSave(1L, 11L, request("김크레딧", "촬영"));

        Map<String, Object> verification = capturedVerification();
        assertThat(verification)
                .containsEntry("verificationStatus", "VERIFIED")
                .containsEntry("providerPersonName", "김크레딧")
                .containsEntry("providerRoleName", "촬영");
    }

    @Test
    void leavesProviderCreditNullWhenNameDoesNotMatch() {
        when(kobisClient.movieDetail("movie-1")).thenReturn(Optional.of(detail("다른 이름", "촬영")));

        service.verifyAfterSave(1L, 11L, request("김크레딧", "촬영"));

        Map<String, Object> verification = capturedVerification();
        assertThat(verification).containsEntry("verificationStatus", "NOT_VERIFIED");
        assertThat(verification.get("providerPersonName")).isNull();
        assertThat(verification.get("providerRoleName")).isNull();
    }

    @Test
    void recordsAmbiguousWhenNameMatchesButRoleDoesNot() {
        when(kobisClient.movieDetail("movie-1")).thenReturn(Optional.of(detail("김크레딧", "배우")));

        service.verifyAfterSave(1L, 11L, request("김크레딧", "촬영"));

        Map<String, Object> verification = capturedVerification();
        assertThat(verification)
                .containsEntry("verificationStatus", "AMBIGUOUS")
                .containsEntry("providerPersonName", "김크레딧")
                .containsEntry("providerRoleName", "배우");
    }

    @Test
    void recordsErrorWithoutBlockingSaveWhenKobisFails() {
        when(kobisClient.movieDetail("movie-1")).thenThrow(new IllegalStateException("KOBIS unavailable"));

        assertThatCode(() -> service.verifyAfterSave(1L, 11L, request("김크레딧", "촬영")))
                .doesNotThrowAnyException();

        Map<String, Object> verification = capturedVerification();
        assertThat(verification).containsEntry("verificationStatus", "ERROR");
        assertThat(verification.get("providerPersonName")).isNull();
        assertThat(verification.get("providerRoleName")).isNull();
    }

    @Test
    void removesVerificationWhenKobisSelectionIsCleared() {
        PortfolioItemRequest manual = new PortfolioItemRequest(
                null, "수동 작품", "촬영", null, "MANUAL", null, null,
                null, null, 0, null, null, null, null, null, null, "김크레딧"
        );

        service.verifyAfterSave(1L, 11L, manual);

        verify(profileMapper).deletePortfolioVerification(11L);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedVerification() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(profileMapper).upsertPortfolioVerification(captor.capture());
        return captor.getValue();
    }

    private PortfolioItemRequest request(String creditName, String roleName) {
        return new PortfolioItemRequest(
                null, "테스트 작품", roleName, null, "PUBLIC_DATA_MANUAL", "KOBIS", "movie-1",
                null, null, 0, "movie-1", "테스트 작품", null, "2026", null, null, creditName
        );
    }

    private KobisMovieDetail detail(String personName, String roleName) {
        return new KobisMovieDetail(
                "movie-1", "테스트 작품", null, "2026", null, "드라마",
                List.of(), List.of(), List.of(new KobisCredit(personName, null, roleName, "STAFF")), "{}"
        );
    }
}
