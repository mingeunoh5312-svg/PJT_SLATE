package com.slate.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class MediaImageServiceTest {

    @TempDir Path tempDir;

    @Test
    void acceptsJpegPngAndWebpSignatures() {
        assertAccepted("a.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        assertAccepted("a.png", "image/png", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0});
        assertAccepted("a.webp", "image/webp", new byte[]{82, 73, 70, 70, 0, 0, 0, 0, 87, 69, 66, 80});
    }

    @Test
    void rejectsDisguisedAndOversizedFiles() {
        Fixture fixture = fixture(null, 1L);
        MockMultipartFile disguised = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> fixture.service.upload(1L, "profile", 3L, disguised)).isInstanceOf(SlateException.class);

        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0xff; oversized[1] = (byte) 0xd8; oversized[2] = (byte) 0xff;
        MockMultipartFile large = new MockMultipartFile("file", "large.jpg", "image/jpeg", oversized);
        assertThatThrownBy(() -> fixture.service.upload(1L, "profile", 3L, large)).isInstanceOf(SlateException.class);
    }

    @Test
    void rejectsAnotherUsersMutation() {
        Fixture fixture = fixture(null, 2L);
        assertThatThrownBy(() -> fixture.service.upload(1L, "profile", 3L, jpeg()))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void supportsOwnedContestAndContestRequestImages() {
        MediaImageMapper mapper = mock(MediaImageMapper.class);
        AuditLogService audit = mock(AuditLogService.class);
        MediaImageService service = new MediaImageService(mapper, audit, tempDir.toString());
        Map<String, Object> target = Map.of(
                "entityId", 7L,
                "ownerUserId", 1L,
                "storedPath", "",
                "visibility", "PRIVATE",
                "status", "PENDING"
        );
        when(mapper.selectTarget("CONTEST_REQUEST", 7L)).thenReturn(target);
        when(mapper.updatePath(eq("CONTEST_REQUEST"), eq(7L), any())).thenReturn(1);

        service.upload(1L, "contest_request", 7L, jpeg());

        verify(mapper).updatePath(eq("CONTEST_REQUEST"), eq(7L), any(String.class));
        assertThatThrownBy(() -> service.upload(2L, "contest_request", 7L, jpeg()))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void replacementAndDeleteKeepDatabaseAndFilesAligned() throws Exception {
        Path old = tempDir.resolve("images/profile/old.jpg");
        Files.createDirectories(old.getParent());
        Files.write(old, new byte[]{1});
        Fixture fixture = fixture("images/profile/old.jpg", 1L);

        fixture.service.upload(1L, "profile", 3L, jpeg());
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.mapper).updatePath(eq("PROFILE"), eq(3L), pathCaptor.capture());
        assertThat(old).doesNotExist();
        assertThat(tempDir.resolve(pathCaptor.getValue())).exists();

        when(fixture.mapper.selectTarget("PROFILE", 3L)).thenReturn(Map.of(
                "entityId", 3L, "ownerUserId", 1L, "storedPath", pathCaptor.getValue(), "visibility", "PUBLIC", "status", "ACTIVE"
        ));
        fixture.service.delete(1L, "profile", 3L);
        assertThat(tempDir.resolve(pathCaptor.getValue())).doesNotExist();
        verify(fixture.mapper).updatePath("PROFILE", 3L, null);
    }

    private void assertAccepted(String name, String contentType, byte[] content) {
        Fixture fixture = fixture(null, 1L);
        fixture.service.upload(1L, "profile", 3L, new MockMultipartFile("file", name, contentType, content));
        verify(fixture.mapper).updatePath(eq("PROFILE"), eq(3L), any(String.class));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    }

    private Fixture fixture(String storedPath, Long ownerUserId) {
        MediaImageMapper mapper = mock(MediaImageMapper.class);
        Map<String, Object> target = new java.util.LinkedHashMap<>();
        target.put("entityId", 3L);
        target.put("ownerUserId", ownerUserId);
        target.put("storedPath", storedPath);
        target.put("visibility", "PUBLIC");
        target.put("status", "ACTIVE");
        when(mapper.selectTarget("PROFILE", 3L)).thenReturn(target);
        when(mapper.updatePath(eq("PROFILE"), eq(3L), any())).thenReturn(1);
        return new Fixture(mapper, new MediaImageService(mapper, mock(AuditLogService.class), tempDir.toString()));
    }

    private record Fixture(MediaImageMapper mapper, MediaImageService service) { }
}
