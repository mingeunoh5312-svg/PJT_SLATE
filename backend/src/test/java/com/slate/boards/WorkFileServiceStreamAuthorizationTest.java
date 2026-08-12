package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import com.slate.teams.TeamMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

class WorkFileServiceStreamAuthorizationTest {

    @TempDir
    private Path uploadRoot;

    private RecordingWorkFileMapper workFileMapper;
    private WorkFileService workFileService;

    @BeforeEach
    void setUp() {
        workFileMapper = new RecordingWorkFileMapper();
        workFileService = new WorkFileService(
                workFileMapper.proxy(),
                teamMapperProxy(),
                null,
                null,
                uploadRoot.toString(),
                "ffprobe"
        );
    }

    @Test
    void anonymousCanStreamPublicPublishedWorkFile() throws Exception {
        createStoredFile("public.mp4");
        workFileMapper.file = file("public.mp4", 10L);
        workFileMapper.accessRows = List.of(Map.of(
                "postVisibility", "PUBLIC",
                "workVisibility", "PUBLIC",
                "postStatus", "PUBLISHED",
                "workStatus", "PUBLISHED"
        ));

        var response = workFileService.stream(1L, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentLength()).isEqualTo(5L);
    }

    @Test
    void anonymousCannotStreamPrivateWorkFile() {
        workFileMapper.file = file("private.mp4", 10L);
        workFileMapper.accessRows = List.of(Map.of(
                "postVisibility", "PRIVATE",
                "workVisibility", "PRIVATE",
                "postStatus", "PUBLISHED",
                "workStatus", "PUBLISHED"
        ));

        assertThatThrownBy(() -> workFileService.stream(1L, null))
                .isInstanceOf(SlateException.class)
                .satisfies(ex -> assertThat(((SlateException) ex).status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void uploaderCanStreamOwnPrivateWorkFile() throws Exception {
        createStoredFile("own.mp4");
        workFileMapper.file = file("own.mp4", 10L);

        var response = workFileService.stream(1L, new CurrentUser(10L, "user@example.com", "사용자", "USER", List.of("ROLE_USER")));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(workFileMapper.streamAccessLookupCount).isZero();
    }

    private void createStoredFile(String filename) throws Exception {
        Files.writeString(uploadRoot.resolve(filename), "video");
    }

    private Map<String, Object> file(String storedPath, Long uploaderUserId) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("fileId", 1L);
        file.put("uploaderUserId", uploaderUserId);
        file.put("status", "ACTIVE");
        file.put("storedPath", storedPath);
        file.put("contentType", "video/mp4");
        file.put("originalName", storedPath);
        return file;
    }

    private TeamMapper teamMapperProxy() {
        InvocationHandler handler = (proxy, method, args) -> null;
        return (TeamMapper) Proxy.newProxyInstance(
                TeamMapper.class.getClassLoader(),
                new Class<?>[]{TeamMapper.class},
                handler
        );
    }

    private static class RecordingWorkFileMapper implements InvocationHandler {

        private Map<String, Object> file = Map.of();
        private List<Map<String, Object>> accessRows = List.of();
        private int streamAccessLookupCount;

        WorkFileMapper proxy() {
            return (WorkFileMapper) Proxy.newProxyInstance(
                    WorkFileMapper.class.getClassLoader(),
                    new Class<?>[]{WorkFileMapper.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "selectFileById" -> file;
                case "selectStreamAccessRows" -> {
                    streamAccessLookupCount++;
                    yield accessRows;
                }
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType.equals(int.class)) {
                return 0;
            }
            if (returnType.equals(long.class)) {
                return 0L;
            }
            return null;
        }
    }
}
