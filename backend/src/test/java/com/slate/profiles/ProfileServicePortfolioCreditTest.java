package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.slate.operations.AuditLogService;
import com.slate.profiles.ProfileController.PortfolioItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProfileServicePortfolioCreditTest {

    private ProfileMapper profileMapper;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        profileMapper = mock(ProfileMapper.class);
        service = new ProfileService(
                profileMapper,
                mock(AuditLogService.class),
                mock(PortfolioVerificationService.class)
        );
    }

    @Test
    void createPersistsTrimmedCreditNameWithPortfolioBody() {
        when(profileMapper.selectProfileByUserId(1L)).thenReturn(Map.of("profileId", 7L));
        when(profileMapper.countActivePortfolioItems(7L)).thenReturn(0);
        doAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("portfolioItemId", 11L);
            return 1;
        }).when(profileMapper).insertPortfolioItem(any());
        when(profileMapper.selectPortfolioItemById(11L)).thenReturn(Map.of("portfolioItemId", 11L, "creditName", "김크레딧"));

        Map<String, Object> created = service.createPortfolioItem(1L, request("  김크레딧  "));

        assertThat(created).containsEntry("creditName", "김크레딧");
        assertThat(capturedItem()).containsEntry("creditName", "김크레딧");
    }

    @Test
    void updateKeepsCreditNameWhenOtherFieldsChange() {
        when(profileMapper.selectOwnedPortfolioItem(1L, 11L)).thenReturn(Map.of(
                "portfolioItemId", 11L, "profileId", 7L, "creditName", "김크레딧"
        ));
        when(profileMapper.updatePortfolioItem(any())).thenReturn(1);
        when(profileMapper.selectPortfolioItemById(11L)).thenReturn(Map.of(
                "portfolioItemId", 11L, "title", "수정 제목", "creditName", "김크레딧"
        ));

        Map<String, Object> updated = service.updatePortfolioItem(1L, 11L, request("김크레딧"));

        assertThat(updated).containsEntry("creditName", "김크레딧");
        assertThat(capturedUpdatedItem()).containsEntry("creditName", "김크레딧");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedItem() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(profileMapper).insertPortfolioItem(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedUpdatedItem() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(profileMapper).updatePortfolioItem(captor.capture());
        return captor.getValue();
    }

    private PortfolioItemRequest request(String creditName) {
        return new PortfolioItemRequest(
                null, "수정 제목", "촬영", "설명", "MANUAL", null, null,
                null, null, 0, null, null, null, null, null, null, creditName
        );
    }
}
