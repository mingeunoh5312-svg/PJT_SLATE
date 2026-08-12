package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import com.slate.operations.RequestLogContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoardServicePostSearchTest {

    private RecordingBoardMapper boardMapper;
    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardMapper = new RecordingBoardMapper();
        boardService = new BoardService(boardMapper.proxy(), null, null, null, null, null, null, new RequestLogContext("test-salt"));
    }

    @Test
    void postsTrimsKeywordAndPassesItToMapper() {
        boardService.posts("WORK", "latest", "  Rick Astley  ", null, "MUSIC_VIDEO", 3L, 200, 7L, false);

        assertThat(boardMapper.category).isEqualTo("WORK");
        assertThat(boardMapper.sort).isEqualTo("latest");
        assertThat(boardMapper.keyword).isEqualTo("Rick Astley");
        assertThat(boardMapper.freeCategory).isNull();
        assertThat(boardMapper.workType).isEqualTo("MUSIC_VIDEO");
        assertThat(boardMapper.genreId).isEqualTo(3L);
        assertThat(boardMapper.limit).isEqualTo(50);
        assertThat(boardMapper.userId).isEqualTo(7L);
        assertThat(boardMapper.admin).isFalse();
    }

    @Test
    void postsConvertsBlankKeywordToNull() {
        boardService.posts("WORK", "latest", "   ", null, null, null, 20, null, false);

        assertThat(boardMapper.keyword).isNull();
    }

    private static final class RecordingBoardMapper implements InvocationHandler {

        private String category;
        private String sort;
        private String keyword;
        private String freeCategory;
        private String workType;
        private Long genreId;
        private Integer limit;
        private Long userId;
        private Boolean admin;

        BoardMapper proxy() {
            return (BoardMapper) Proxy.newProxyInstance(
                    BoardMapper.class.getClassLoader(),
                    new Class<?>[] {BoardMapper.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("selectPosts".equals(method.getName())) {
                category = (String) args[0];
                sort = (String) args[1];
                keyword = (String) args[2];
                freeCategory = (String) args[3];
                workType = (String) args[4];
                genreId = (Long) args[5];
                limit = (Integer) args[6];
                userId = (Long) args[7];
                admin = (Boolean) args[8];
                return List.of();
            }
            return defaultValue(method.getReturnType());
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(returnType)) {
                return List.of();
            }
            return null;
        }
    }
}
