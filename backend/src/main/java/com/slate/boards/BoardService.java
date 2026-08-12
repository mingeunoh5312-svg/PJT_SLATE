package com.slate.boards;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.boards.BoardController.BoardPostRequest;
import com.slate.boards.BoardController.ReviewRequest;
import com.slate.boards.BoardController.TeamWorkDecisionRequest;
import com.slate.boards.BoardController.WorkRequest;
import com.slate.common.SlateException;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import com.slate.operations.RequestLogContext;
import com.slate.security.CurrentUser;
import com.slate.teams.TeamMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BoardService {

    private static final List<String> POST_CATEGORIES = List.of("WORK", "FREE");
    private static final List<String> FREE_CATEGORIES = List.of("NOTICE", "QUESTION", "INFO", "REVIEW", "FREE");
    private static final List<String> WORK_TYPES = List.of(
            "SHORT_FILM", "FEATURE_FILM", "MUSIC_VIDEO", "ADVERTISEMENT", "DOCUMENTARY", "WEB_CONTENT", "OTHER"
    );
    private static final List<String> POST_VISIBILITIES = List.of("PUBLIC", "COMPANY", "PRIVATE");
    private static final List<String> WORK_MEDIA_TYPES = List.of("MANUAL", "YOUTUBE", "SERVER_UPLOAD");

    private final BoardMapper boardMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final WorkFileService workFileService;
    private final TeamMapper teamMapper;
    private final NotificationService notificationService;
    private final YoutubeClient youtubeClient;
    private final RequestLogContext requestLogContext;

    public BoardService(
            BoardMapper boardMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            WorkFileService workFileService,
            TeamMapper teamMapper,
            NotificationService notificationService,
            YoutubeClient youtubeClient,
            RequestLogContext requestLogContext
    ) {
        this.boardMapper = boardMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.workFileService = workFileService;
        this.teamMapper = teamMapper;
        this.notificationService = notificationService;
        this.youtubeClient = youtubeClient;
        this.requestLogContext = requestLogContext;
    }

    public List<Map<String, Object>> posts(
            String category,
            String sort,
            String keyword,
            String freeCategory,
            String workType,
            Long genreId,
            Integer limit,
            Long userId,
            boolean admin
    ) {
        String normalizedCategory = normalizeListCategory(category);
        String normalizedSort = normalizeSort(sort);
        String normalizedFreeCategory = normalizeListFreeCategory(normalizedCategory, freeCategory);
        String normalizedWorkType = normalizeListWorkType(normalizedCategory, workType);
        Long normalizedGenreId = normalizeGenreFilter(normalizedCategory, genreId);
        return boardMapper.selectPosts(
                normalizedCategory,
                normalizedSort,
                textOrNull(keyword),
                normalizedFreeCategory,
                normalizedWorkType,
                normalizedGenreId,
                safeLimit(limit),
                userId,
                admin
        ).stream()
                .map(this::withRepresentativeImageField)
                .toList();
    }

    public List<Map<String, Object>> myWorks(Long userId, Integer limit) {
        if (userId == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return boardMapper.selectMyWorks(userId, safeLimit(limit)).stream()
                .map(this::withRepresentativeImageField)
                .toList();
    }

    @Transactional
    public Map<String, Object> post(Long postId, Long userId, boolean admin, HttpServletRequest request) {
        Map<String, Object> post = requirePost(postId, userId, admin);
        if (registerView(postId, userId, request)) {
            post = requirePost(postId, userId, admin);
        }
        return enrich(post);
    }

    @Transactional
    public Map<String, Object> createPost(CurrentUser currentUser, BoardPostRequest request) {
        Long userId = currentUser.userId();
        String category = normalizePostCategory(request.category());
        Map<String, Object> post = postMap(userId, null, category, request, currentUser.isAdmin());
        boardMapper.insertPost(post);
        Long postId = ((Number) post.get("postId")).longValue();
        if ("WORK".equals(category) && request.work() != null) {
            assertTeamWorkPublishAllowed(userId, request.work().teamId());
            Map<String, Object> work = workMap(userId, postId, request.title(), request.content(), request.work());
            boardMapper.insertWork(work);
            replaceWorkGenres(longValue(work.get("workId")), normalizeGenreIds(request.work().genreIds()));
        }
        Map<String, Object> created = enrich(requirePost(postId, userId, false));
        auditLogService.recordAudit(userId, "BOARD_POST_CREATED", "BOARD_POST", postId, null, auditPayload(created));
        return created;
    }

    @Transactional
    public Map<String, Object> updatePost(CurrentUser currentUser, Long postId, BoardPostRequest request) {
        Map<String, Object> existing = requirePost(postId, currentUser.userId(), currentUser.isAdmin());
        assertPostOwnerOrAdmin(currentUser, existing);
        String category = normalizePostCategory(request.category());
        Map<String, Object> post = postMap(currentUser.userId(), postId, category, request, currentUser.isAdmin());
        if (boardMapper.updatePost(post) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        if ("WORK".equals(category) && request.work() != null) {
            assertTeamWorkPublishAllowed(currentUser.userId(), request.work().teamId());
            Map<String, Object> work = boardMapper.selectWorkByPostId(postId);
            Map<String, Object> workData = workMap(currentUser.userId(), postId, request.title(), request.content(), request.work());
            if (work == null) {
                boardMapper.insertWork(workData);
            } else {
                workData.put("workId", work.get("workId"));
                boardMapper.updateWork(workData);
            }
            replaceWorkGenres(longValue(workData.get("workId")), normalizeGenreIds(request.work().genreIds()));
        }
        Map<String, Object> updated = enrich(requirePost(postId, currentUser.userId(), currentUser.isAdmin()));
        auditLogService.recordAudit(currentUser.userId(), "BOARD_POST_UPDATED", "BOARD_POST", postId, auditPayload(existing), auditPayload(updated));
        return updated;
    }

    @Transactional
    public Map<String, Object> deletePost(CurrentUser currentUser, Long postId) {
        Map<String, Object> existing = requirePost(postId, currentUser.userId(), currentUser.isAdmin());
        assertPostOwnerOrAdmin(currentUser, existing);
        String status = currentUser.isAdmin() ? "ADMIN_DELETED" : "AUTHOR_DELETED";
        boardMapper.softDeletePost(postId, status);
        boardMapper.softDeleteWorkByPostId(postId);
        auditLogService.recordAudit(currentUser.userId(), "BOARD_POST_" + status, "BOARD_POST", postId, auditPayload(existing), Map.of("status", status));
        if ("ADMIN_DELETED".equals(status)) {
            auditLogService.recordOperation(
                    "WARN",
                    "CONTENT_MODERATED",
                    "관리자가 게시글을 삭제했습니다.",
                    Map.of("postId", postId, "adminUserId", currentUser.userId())
            );
        }
        return existing;
    }

    public List<Map<String, Object>> reviews(Long postId) {
        return boardMapper.selectReviewsByPostId(postId);
    }

    @Transactional
    public Map<String, Object> createReview(Long userId, Long postId, ReviewRequest request) {
        requirePost(postId, userId, false);
        Map<String, Object> review = reviewMap(userId, postId, null, request);
        boardMapper.insertReview(review);
        boardMapper.recountReviewCount(postId);
        Map<String, Object> created = boardMapper.selectReviewById(((Number) review.get("reviewId")).longValue());
        auditLogService.recordAudit(userId, "BOARD_REVIEW_CREATED", "BOARD_REVIEW", ((Number) created.get("reviewId")).longValue(), null, auditPayload(created));
        return created;
    }

    @Transactional
    public Map<String, Object> updateReview(CurrentUser currentUser, Long reviewId, ReviewRequest request) {
        Map<String, Object> existing = requireReview(reviewId);
        assertPublishedReview(existing);
        assertReviewOwnerOrAdmin(currentUser, existing);
        if (boardMapper.countReviewReplies(reviewId) > 0) {
            throw new SlateException("답글이 있는 리뷰는 수정할 수 없습니다.");
        }
        boardMapper.updateReview(reviewId, request.content().trim());
        Map<String, Object> updated = boardMapper.selectReviewById(reviewId);
        auditLogService.recordAudit(currentUser.userId(), "BOARD_REVIEW_UPDATED", "BOARD_REVIEW", reviewId, auditPayload(existing), auditPayload(updated));
        return updated;
    }

    @Transactional
    public Map<String, Object> deleteReview(CurrentUser currentUser, Long reviewId) {
        Map<String, Object> existing = requireReview(reviewId);
        assertPublishedReview(existing);
        assertReviewOwnerOrAdmin(currentUser, existing);
        String status = currentUser.isAdmin() ? "ADMIN_DELETED" : "AUTHOR_DELETED";
        String displayText = currentUser.isAdmin() ? "관리자에 의해 삭제된 댓글입니다." : "삭제된 댓글입니다.";
        boardMapper.softDeleteReview(reviewId, status, displayText);
        boardMapper.recountReviewCount(((Number) existing.get("postId")).longValue());
        Map<String, Object> deleted = boardMapper.selectReviewById(reviewId);
        auditLogService.recordAudit(currentUser.userId(), "BOARD_REVIEW_" + status, "BOARD_REVIEW", reviewId, auditPayload(existing), auditPayload(deleted));
        if ("ADMIN_DELETED".equals(status)) {
            auditLogService.recordOperation(
                    "WARN",
                    "CONTENT_MODERATED",
                    "관리자가 리뷰를 삭제했습니다.",
                    Map.of("reviewId", reviewId, "adminUserId", currentUser.userId())
            );
        }
        return deleted;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        requirePost(postId, userId, false);
        String activeYn = boardMapper.selectLikeActiveYn(postId, userId);
        boolean nextActive = !"Y".equals(activeYn);
        if (activeYn == null) {
            boardMapper.insertLike(postId, userId);
        } else {
            boardMapper.updateLike(postId, userId, nextActive ? "Y" : "N");
        }
        boardMapper.recountLikeCount(postId);
        Number likeCount = boardMapper.selectLikeCount(postId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("postId", postId);
        result.put("active", nextActive);
        result.put("likeCount", likeCount == null ? 0 : likeCount.intValue());
        auditLogService.recordAudit(userId, nextActive ? "BOARD_POST_LIKED" : "BOARD_POST_UNLIKED", "BOARD_POST", postId, null, result);
        return result;
    }

    public List<Map<String, Object>> rankings(String type, String workType, Long genreId, Integer limit, Long currentUserId) {
        int safeLimit = safeLimit(limit);
        String normalizedType = normalizeRankingType(type);
        return switch (normalizedType) {
            case "POPULAR_PROFILE" -> boardMapper.selectPopularProfileRanking(currentUserId, safeLimit);
            case "POPULAR_TEAM" -> boardMapper.selectPopularTeamRanking(safeLimit);
            case "WEEKLY_WORK", "MONTHLY_WORK", "POPULAR_WORK" -> boardMapper.selectWorkRanking(
                    normalizedType,
                    normalizeOptionalWorkType(workType),
                    positiveIdOrNull(genreId, "장르"),
                    currentUserId,
                    safeLimit
            ).stream()
                    .map(this::withRepresentativeImageField)
                    .toList();
            default -> throw new SlateException("지원하지 않는 랭킹 유형입니다.");
        };
    }

    public List<Map<String, Object>> myTeamWorkRequests(Long userId) {
        return boardMapper.selectTeamWorkRequestsByRequester(userId);
    }

    public List<Map<String, Object>> teamWorkRequests(Long userId, Long teamId) {
        assertTeamManager(userId, teamId);
        return boardMapper.selectTeamWorkRequestsByTeam(teamId);
    }

    @Transactional
    public Map<String, Object> createTeamWorkRequest(Long userId, BoardPostRequest request) {
        String category = normalizePostCategory(request.category());
        if (!"WORK".equals(category) || request.work() == null || request.work().teamId() == null) {
            throw new SlateException("팀 작업물 승인 요청에는 팀 작업물 정보가 필요합니다.");
        }
        Long teamId = request.work().teamId();
        assertActiveTeamMember(userId, teamId);
        assertTeamWorkTargetOpen(teamId);
        WorkRequest workRequest = request.work();
        String youtubeUrl = cleanYoutubeUrl(workRequest.youtubeUrl());
        Long fileId = workRequest.fileId();
        if (StringUtils.hasText(youtubeUrl) && fileId != null) {
            throw new SlateException("유튜브 URL과 서버 업로드 파일은 동시에 연결할 수 없습니다.");
        }
        workFileService.assertFileUsable(userId, teamId, fileId);
        if (fileId != null && boardMapper.countTeamWorkRequestByFile(fileId) > 0) {
            throw new SlateException("이미 팀 작업물 승인 요청에 사용된 파일입니다.");
        }
        Map<String, Object> approval = teamWorkRequestMap(userId, request);
        boardMapper.insertTeamWorkRequest(approval);
        Long requestId = longValue(approval.get("requestId"));
        insertApprovalGenres(requestId, normalizeGenreIds(workRequest.genreIds()));
        Map<String, Object> created = requireTeamWorkRequest(requestId);
        notificationService.sendToTeamManagers(
                teamId,
                userId,
                "팀 작업물 승인 요청",
                textOrDefault(request.title(), "팀 작업물") + " 공개 승인을 기다립니다.",
                "TEAM_WORK_REQUEST",
                requestId
        );
        auditLogService.recordAudit(userId, "TEAM_WORK_REQUEST_CREATED", "TEAM_WORK_REQUEST", requestId, null, auditPayload(created));
        auditLogService.recordOperation(
                "INFO",
                "TEAM_WORK_REQUEST_CREATED",
                "팀 작업물 승인 요청이 접수되었습니다.",
                Map.of("requestId", requestId, "teamId", teamId, "requesterUserId", userId)
        );
        return created;
    }

    @Transactional
    public Map<String, Object> decideTeamWorkRequest(Long deciderUserId, Long requestId, TeamWorkDecisionRequest request) {
        Map<String, Object> existing = requireTeamWorkRequest(requestId);
        Long teamId = longValue(existing.get("teamId"));
        assertTeamManager(deciderUserId, teamId);
        if (!"PENDING".equals(existing.get("status"))) {
            throw new SlateException("이미 처리된 팀 작업물 요청입니다.");
        }
        String decision = textOrDefault(request.decision(), "").toUpperCase();
        if (!List.of("APPROVED", "REJECTED").contains(decision)) {
            throw new SlateException("결정값은 APPROVED 또는 REJECTED만 허용합니다.");
        }
        Map<String, Object> decisionRow = new LinkedHashMap<>();
        decisionRow.put("requestId", requestId);
        decisionRow.put("status", decision);
        decisionRow.put("rejectReason", "REJECTED".equals(decision) ? textOrDefault(request.reason(), "팀 작업물 공개가 거절되었습니다.") : null);
        decisionRow.put("decidedBy", deciderUserId);
        decisionRow.put("boardPostId", null);
        decisionRow.put("workId", null);
        if ("APPROVED".equals(decision)) {
            publishApprovedTeamWork(existing, decisionRow);
        }
        if (boardMapper.decideTeamWorkRequest(decisionRow) == 0) {
            throw new SlateException("팀 작업물 요청을 처리하지 못했습니다.");
        }
        Map<String, Object> decided = requireTeamWorkRequest(requestId);
        Long requesterUserId = longValue(existing.get("requesterUserId"));
        notificationService.send(
                requesterUserId,
                deciderUserId,
                "TEAM",
                "APPROVED".equals(decision) ? "팀 작업물이 승인되었습니다." : "팀 작업물이 거절되었습니다.",
                "APPROVED".equals(decision)
                        ? textOrDefault((String) existing.get("title"), "팀 작업물") + " 게시글이 공개되었습니다."
                        : textOrDefault(request.reason(), "팀 작업물 공개 요청이 거절되었습니다."),
                "TEAM_WORK_REQUEST",
                requestId
        );
        auditLogService.recordAudit(deciderUserId, "TEAM_WORK_REQUEST_" + decision, "TEAM_WORK_REQUEST", requestId, auditPayload(existing), auditPayload(decided));
        auditLogService.recordOperation(
                "INFO",
                "TEAM_WORK_REQUEST_" + decision,
                "팀 작업물 승인 요청을 처리했습니다.",
                Map.of("requestId", requestId, "teamId", teamId, "deciderUserId", deciderUserId, "status", decision)
        );
        return decided;
    }

    private Map<String, Object> enrich(Map<String, Object> post) {
        Long postId = ((Number) post.get("postId")).longValue();
        Map<String, Object> result = new LinkedHashMap<>(post);
        result.putIfAbsent("representativeImageUrl", null);
        Map<String, Object> work = boardMapper.selectWorkByPostId(postId);
        if (work != null) {
            work = withRepresentativeImageField(work);
            work.put("genres", boardMapper.selectWorkGenresByWorkId(longValue(work.get("workId"))));
        }
        result.put("work", work);
        result.put("reviews", boardMapper.selectReviewsByPostId(postId));
        return result;
    }

    private Map<String, Object> withRepresentativeImageField(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.putIfAbsent("representativeImageUrl", null);
        return result;
    }

    private boolean registerView(Long postId, Long userId, HttpServletRequest request) {
        String ipHash = hashViewIdentity(userId, request);
        if (boardMapper.countRecentView(postId, userId, ipHash) > 0) {
            return false;
        }
        boardMapper.insertViewLog(postId, userId, ipHash, LocalDateTime.now());
        boardMapper.incrementViewCount(postId);
        return true;
    }

    private Map<String, Object> postMap(
            Long userId,
            Long postId,
            String category,
            BoardPostRequest request,
            boolean admin
    ) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("postId", postId);
        post.put("authorUserId", userId);
        post.put("category", category);
        post.put("freeCategory", normalizeWriteFreeCategory(category, request.freeCategory(), admin));
        post.put("title", request.title().trim());
        post.put("content", request.content().trim());
        post.put("visibility", normalizeVisibility(request.visibility()));
        post.put("status", "PUBLISHED");
        return post;
    }

    private Map<String, Object> workMap(Long userId, Long postId, String postTitle, String postContent, WorkRequest request) {
        String youtubeUrl = cleanYoutubeUrl(request.youtubeUrl());
        Long fileId = request.fileId();
        if (StringUtils.hasText(youtubeUrl) && fileId != null) {
            throw new SlateException("유튜브 URL과 서버 업로드 파일은 동시에 연결할 수 없습니다.");
        }
        workFileService.assertFileUsable(userId, request.teamId(), fileId);
        YoutubeVideoMetadata youtubeMetadata = resolveYoutubeMetadata(youtubeUrl, fileId);
        String mediaType = fileId != null ? "SERVER_UPLOAD" : youtubeMetadata != null ? "YOUTUBE" : normalizeWorkMediaType(request.mediaType());
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("ownerUserId", userId);
        work.put("teamId", request.teamId());
        work.put("boardPostId", postId);
        work.put("fileId", fileId);
        work.put("title", textOrDefault(request.title(), postTitle));
        work.put("description", textOrDefault(request.description(), postContent));
        work.put("mediaType", mediaType);
        work.put("workType", normalizeWorkType(request.workType()));
        work.put("youtubeUrl", youtubeMetadata == null ? null : youtubeMetadata.embedUrl());
        putYoutubeMetadata(work, youtubeMetadata);
        work.put("visibility", normalizeVisibility(textOrDefault(request.visibility(), "PUBLIC")));
        work.put("status", "PUBLISHED");
        return work;
    }

    private Map<String, Object> teamWorkRequestMap(Long userId, BoardPostRequest request) {
        WorkRequest work = request.work();
        Long fileId = work.fileId();
        String youtubeUrl = cleanYoutubeUrl(work.youtubeUrl());
        if (StringUtils.hasText(youtubeUrl) && fileId != null) {
            throw new SlateException("유튜브 URL과 서버 업로드 파일은 동시에 연결할 수 없습니다.");
        }
        YoutubeVideoMetadata youtubeMetadata = resolveYoutubeMetadata(youtubeUrl, fileId);
        String mediaType = fileId != null ? "SERVER_UPLOAD" : youtubeMetadata != null ? "YOUTUBE" : normalizeWorkMediaType(work.mediaType());
        Map<String, Object> approval = new LinkedHashMap<>();
        approval.put("teamId", work.teamId());
        approval.put("requesterUserId", userId);
        approval.put("fileId", fileId);
        approval.put("title", request.title().trim());
        approval.put("content", request.content().trim());
        approval.put("mediaType", mediaType);
        approval.put("workType", normalizeWorkType(work.workType()));
        approval.put("youtubeUrl", youtubeMetadata == null ? null : youtubeMetadata.embedUrl());
        putYoutubeMetadata(approval, youtubeMetadata);
        approval.put("visibility", normalizeVisibility(textOrDefault(work.visibility(), request.visibility())));
        return approval;
    }

    private void publishApprovedTeamWork(Map<String, Object> approval, Map<String, Object> decisionRow) {
        Long requesterUserId = longValue(approval.get("requesterUserId"));
        Long teamId = longValue(approval.get("teamId"));
        Long fileId = longValue(approval.get("fileId"));
        String title = (String) approval.get("title");
        String content = (String) approval.get("content");
        String visibility = normalizeVisibility((String) approval.get("visibility"));
        workFileService.assertFileUsable(requesterUserId, teamId, fileId);
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("postId", null);
        post.put("authorUserId", requesterUserId);
        post.put("category", "WORK");
        post.put("title", title);
        post.put("content", content);
        post.put("visibility", visibility);
        post.put("status", "PUBLISHED");
        boardMapper.insertPost(post);
        Long postId = longValue(post.get("postId"));
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("ownerUserId", requesterUserId);
        work.put("teamId", teamId);
        work.put("boardPostId", postId);
        work.put("fileId", fileId);
        work.put("title", title);
        work.put("description", content);
        work.put("mediaType", approval.get("mediaType"));
        work.put("workType", normalizeWorkType((String) approval.get("workType")));
        work.put("youtubeUrl", fileId == null ? approval.get("youtubeUrl") : null);
        copyYoutubeMetadata(work, approval);
        work.put("visibility", visibility);
        work.put("status", "PUBLISHED");
        boardMapper.insertWork(work);
        Long workId = longValue(work.get("workId"));
        for (Map<String, Object> genre : boardMapper.selectTeamWorkApprovalGenres(longValue(approval.get("requestId")))) {
            boardMapper.insertWorkGenre(workId, longValue(genre.get("genreId")), ((Number) genre.get("sortOrder")).intValue());
        }
        decisionRow.put("boardPostId", postId);
        decisionRow.put("workId", workId);
    }

    private Map<String, Object> reviewMap(Long userId, Long postId, Long reviewId, ReviewRequest request) {
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("reviewId", reviewId);
        review.put("postId", postId);
        review.put("authorUserId", userId);
        review.put("parentReviewId", request.parentReviewId());
        review.put("content", request.content().trim());
        review.put("status", "PUBLISHED");
        return review;
    }

    private Map<String, Object> requirePost(Long postId, Long userId, boolean admin) {
        Map<String, Object> post = boardMapper.selectPostById(postId, userId, admin);
        if (post == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private Map<String, Object> requireTeamWorkRequest(Long requestId) {
        Map<String, Object> request = boardMapper.selectTeamWorkRequestById(requestId);
        if (request == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀 작업물 승인 요청을 찾을 수 없습니다.");
        }
        return request;
    }

    private Map<String, Object> requireReview(Long reviewId) {
        Map<String, Object> review = boardMapper.selectReviewById(reviewId);
        if (review == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다.");
        }
        return review;
    }

    private void assertPostOwnerOrAdmin(CurrentUser currentUser, Map<String, Object> post) {
        Long authorUserId = ((Number) post.get("authorUserId")).longValue();
        if (authorUserId.equals(currentUser.userId())) {
            return;
        }
        if (currentUser.isAdmin()) {
            adminPermissionService.require(currentUser.userId(), AdminPermissionCatalog.CONTENT_MODERATION);
            return;
        }
        throw new SlateException(HttpStatus.FORBIDDEN, "게시글 수정 권한이 없습니다.");
    }

    private void assertReviewOwnerOrAdmin(CurrentUser currentUser, Map<String, Object> review) {
        Long authorUserId = ((Number) review.get("authorUserId")).longValue();
        if (authorUserId.equals(currentUser.userId())) {
            return;
        }
        if (currentUser.isAdmin()) {
            adminPermissionService.require(currentUser.userId(), AdminPermissionCatalog.CONTENT_MODERATION);
            return;
        }
        throw new SlateException(HttpStatus.FORBIDDEN, "리뷰 수정 권한이 없습니다.");
    }

    private void assertTeamWorkPublishAllowed(Long userId, Long teamId) {
        if (teamId == null) {
            return;
        }
        assertTeamWorkTargetOpen(teamId);
        if (!isTeamManager(userId, teamId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀 작업물은 팀장 또는 부팀장 승인 후 게시할 수 있습니다.");
        }
    }

    private void assertActiveTeamMember(Long userId, Long teamId) {
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        if (!StringUtils.hasText(role)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀 작업물은 팀 멤버만 요청할 수 있습니다.");
        }
    }

    private void assertTeamManager(Long userId, Long teamId) {
        if (!isTeamManager(userId, teamId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀장 또는 부팀장만 팀 작업물 요청을 처리할 수 있습니다.");
        }
    }

    private boolean isTeamManager(Long userId, Long teamId) {
        String role = teamMapper.selectActiveTeamRole(teamId, userId);
        return "LEADER".equals(role) || "SUB_LEADER".equals(role);
    }

    private void assertTeamWorkTargetOpen(Long teamId) {
        Map<String, Object> team = teamMapper.selectTeamById(teamId);
        if (team == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다.");
        }
        if ("ENDED".equals(team.get("status"))) {
            throw new SlateException("종료된 팀에는 작업물을 등록할 수 없습니다.");
        }
    }

    private void assertPublishedReview(Map<String, Object> review) {
        if (!"PUBLISHED".equals(review.get("status"))) {
            throw new SlateException("이미 삭제된 리뷰입니다.");
        }
    }

    private String normalizePostCategory(String category) {
        String value = textOrDefault(category, "WORK").toUpperCase();
        if (!POST_CATEGORIES.contains(value)) {
            throw new SlateException("작성 가능한 게시판은 WORK 또는 FREE입니다.");
        }
        return value;
    }

    private String normalizeListCategory(String category) {
        String value = textOrDefault(category, "WORK").toUpperCase();
        if (!POST_CATEGORIES.contains(value)) {
            throw new SlateException("조회 가능한 게시판은 WORK 또는 FREE입니다.");
        }
        return value;
    }

    private String normalizeRankingType(String type) {
        String value = textOrDefault(type, "POPULAR_WORK").toUpperCase();
        if (!List.of("POPULAR_WORK", "WEEKLY_WORK", "MONTHLY_WORK", "POPULAR_PROFILE", "POPULAR_TEAM").contains(value)) {
            throw new SlateException("지원하지 않는 랭킹 유형입니다.");
        }
        return value;
    }

    private String normalizeSort(String sort) {
        String value = textOrDefault(sort, "reaction").toLowerCase();
        if (!List.of("latest", "likes", "views", "reaction").contains(value)) {
            throw new SlateException("지원하지 않는 게시글 정렬입니다.");
        }
        return value;
    }

    private String normalizeWriteFreeCategory(String category, String freeCategory, boolean admin) {
        if (!"FREE".equals(category)) {
            if (StringUtils.hasText(freeCategory)) {
                throw new SlateException("작업물 게시글에는 자유게시판 분류를 지정할 수 없습니다.");
            }
            return null;
        }
        String value = textOrDefault(freeCategory, null);
        if (value == null || !FREE_CATEGORIES.contains(value.toUpperCase())) {
            throw new SlateException("자유게시판 세부 분류를 선택해주세요.");
        }
        value = value.toUpperCase();
        if ("NOTICE".equals(value) && !admin) {
            throw new SlateException(HttpStatus.FORBIDDEN, "공지 게시글은 관리자만 작성할 수 있습니다.");
        }
        return value;
    }

    private String normalizeListFreeCategory(String category, String freeCategory) {
        if (!StringUtils.hasText(freeCategory)) return null;
        if (!"FREE".equals(category)) {
            throw new SlateException("자유게시판 분류는 FREE 게시판에서만 사용할 수 있습니다.");
        }
        String value = freeCategory.trim().toUpperCase();
        if (!FREE_CATEGORIES.contains(value)) {
            throw new SlateException("지원하지 않는 자유게시판 분류입니다.");
        }
        return value;
    }

    private String normalizeListWorkType(String category, String workType) {
        if (!StringUtils.hasText(workType)) return null;
        if (!"WORK".equals(category)) {
            throw new SlateException("작품 유형은 WORK 게시판에서만 사용할 수 있습니다.");
        }
        return normalizeOptionalWorkType(workType);
    }

    private Long normalizeGenreFilter(String category, Long genreId) {
        Long normalized = positiveIdOrNull(genreId, "장르");
        if (normalized != null && !"WORK".equals(category)) {
            throw new SlateException("장르 필터는 WORK 게시판에서만 사용할 수 있습니다.");
        }
        return normalized;
    }

    private Long positiveIdOrNull(Long value, String fieldName) {
        if (value == null) return null;
        if (value <= 0) throw new SlateException(fieldName + " 식별자가 올바르지 않습니다.");
        return value;
    }

    private List<Long> normalizeGenreIds(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return List.of();
        List<Long> normalized = genreIds.stream()
                .map(id -> positiveIdOrNull(id, "장르"))
                .distinct()
                .toList();
        if (boardMapper.countActiveGenres(normalized) != normalized.size()) {
            throw new SlateException("사용할 수 없는 장르가 포함되어 있습니다.");
        }
        return normalized;
    }

    private void replaceWorkGenres(Long workId, List<Long> genreIds) {
        boardMapper.deleteWorkGenres(workId);
        for (int index = 0; index < genreIds.size(); index++) {
            boardMapper.insertWorkGenre(workId, genreIds.get(index), index);
        }
    }

    private void insertApprovalGenres(Long requestId, List<Long> genreIds) {
        for (int index = 0; index < genreIds.size(); index++) {
            boardMapper.insertTeamWorkApprovalGenre(requestId, genreIds.get(index), index);
        }
    }

    private String normalizeOptionalWorkType(String workType) {
        if (!StringUtils.hasText(workType)) return null;
        String value = workType.trim().toUpperCase();
        if (!WORK_TYPES.contains(value)) {
            throw new SlateException("지원하지 않는 작품 유형입니다.");
        }
        return value;
    }

    private String normalizeWorkType(String workType) {
        return StringUtils.hasText(workType) ? normalizeOptionalWorkType(workType) : "OTHER";
    }

    private String normalizeVisibility(String visibility) {
        String value = textOrDefault(visibility, "PUBLIC").toUpperCase();
        return POST_VISIBILITIES.contains(value) ? value : "PUBLIC";
    }

    private String normalizeWorkMediaType(String mediaType) {
        String value = textOrDefault(mediaType, "MANUAL").toUpperCase();
        if (!WORK_MEDIA_TYPES.contains(value)) {
            throw new SlateException("지원하지 않는 작업물 매체 유형입니다.");
        }
        return value;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 20 : limit, 50));
    }

    private String cleanYoutubeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        return url.trim();
    }

    private YoutubeVideoMetadata resolveYoutubeMetadata(String youtubeUrl, Long fileId) {
        if (fileId != null || !StringUtils.hasText(youtubeUrl)) {
            return null;
        }
        return youtubeClient.fetchMetadata(youtubeUrl);
    }

    private void putYoutubeMetadata(Map<String, Object> target, YoutubeVideoMetadata metadata) {
        target.put("youtubeVideoId", metadata == null ? null : metadata.videoId());
        target.put("youtubeTitle", metadata == null ? null : metadata.title());
        target.put("youtubeChannelTitle", metadata == null ? null : metadata.channelTitle());
        target.put("youtubeThumbnailUrl", metadata == null ? null : metadata.thumbnailUrl());
        target.put("youtubeDurationSeconds", metadata == null ? null : metadata.durationSeconds());
    }

    private void copyYoutubeMetadata(Map<String, Object> target, Map<String, Object> source) {
        target.put("youtubeVideoId", source.get("youtubeVideoId"));
        target.put("youtubeTitle", source.get("youtubeTitle"));
        target.put("youtubeChannelTitle", source.get("youtubeChannelTitle"));
        target.put("youtubeThumbnailUrl", source.get("youtubeThumbnailUrl"));
        target.put("youtubeDurationSeconds", source.get("youtubeDurationSeconds"));
    }

    private String hashViewIdentity(Long userId, HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwardedFor) ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        String userAgent = textOrDefault(request.getHeader("User-Agent"), "unknown");
        String raw = userId == null ? "GUEST|" + ip + "|" + userAgent : "USER|" + userId;
        return requestLogContext.hashValue(raw);
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private Map<String, Object> auditPayload(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "postId", "reviewId", "requestId", "teamId", "requesterUserId", "authorUserId",
                "category", "title", "status", "visibility", "mediaType", "fileId", "boardPostId",
                "workId", "likeCount", "reviewCount"
        )) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        return result;
    }
}
