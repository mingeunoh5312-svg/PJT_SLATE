package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicProfileContractTest {

    @Mock ProfileMapper profileMapper;
    @Mock AuditLogService auditLogService;
    @Mock PortfolioVerificationService verificationService;

    @Test
    void publicProfileMapperEnforcesVisibilityCompletionAndActivity() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mappers/ProfileMapper.xml"));

        assertThat(xml).contains("<select id=\"selectPublicProfileById\"");
        assertThat(xml).contains("p.visibility = 'PUBLIC'");
        assertThat(xml).contains("p.profile_completed_yn = 'Y'");
        assertThat(xml).contains("p.activity_status = 'VISIBLE'");
        assertThat(xml).contains("u.account_status = 'ACTIVE'");
    }

    @Test
    void publicProfileResponseDoesNotExposeEmail() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("profileId", 3L);
        row.put("userId", 4L);
        row.put("email", "private@example.com");
        row.put("displayName", "공개 사용자");
        when(profileMapper.selectPublicProfileById(3L)).thenReturn(row);
        when(profileMapper.selectProfileRoles(3L)).thenReturn(List.of());
        when(profileMapper.selectProfileGenres(3L)).thenReturn(List.of());
        when(profileMapper.selectProfileConditions(3L)).thenReturn(List.of());
        when(profileMapper.selectPortfolioItems(3L)).thenReturn(List.of());

        ProfileService service = new ProfileService(profileMapper, auditLogService, verificationService);
        Map<String, Object> result = service.publicByProfileId(3L);

        assertThat(result).doesNotContainKey("email");
        assertThat(result).containsEntry("profileId", 3L);
    }
}
