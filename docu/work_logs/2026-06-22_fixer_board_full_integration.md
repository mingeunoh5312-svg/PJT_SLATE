# 게시판 검색·분류·랭킹·팔로우 통합 수정 작업 로그

## 작업 범위

- 게시판 HOME/WORK/FREE/POPULAR 탭과 검색·정렬·분류 통합
- 화면 하드코딩 게시글·작업물·프로필·순위 제거
- 자유게시판 5개 세부 분류와 작업물 종류 DB/API/UI 계약 추가
- 좋아요 기반 인기 작업물과 팔로워 기반 인기 프로필 구현
- 공개 프로필 route/API와 기존 follow API 연결
- 작업물 대표 이미지 우선순위 및 상단 관리 action 정리

## 요구사항별 결과

| 번호 | 결과 |
|---:|---|
| 1 | HOME에 WORK/FREE 범위 선택 검색을 추가하고 실제 게시글 API만 조회한다. 동일 범위·검색어 중복 제출을 막고 범위 변경 시 결과를 초기화한다. |
| 2 | `sampleWorks`, `sampleFreePosts`, 기간별 인기 작업물, 인기 프로필 및 관련 샘플 이미지 import를 제거했다. 오류·빈 배열은 샘플로 대체하지 않는다. |
| 3 | `activeTab` 기본값을 HOME으로 변경하고 탭 active 판단을 하나로 통합했다. 승인 도구는 보조 panel 상태로만 유지한다. |
| 4 | HOME, WORK 목록, 상세, 인기에서 업로드 대표 이미지 > YouTube 썸네일 > 단일 공통 작업물 이미지 순서를 사용하고 깨진 URL도 공통 이미지로 복구한다. 기존 서버 이미지 검증·소유권 구현을 재사용한다. |
| 5 | 글쓰기, 작업물 올리기, 내 업로드 파일 관리, 팀 작업물 승인을 상단 action에 배치하고 중복 quick card를 제거했다. 파일 관리는 로그인 사용자, 승인은 관리 팀이 있는 사용자에게만 표시한다. |
| 6 | 인기 작업물 SQL을 `like_count DESC, created_at DESC, post_id DESC`로 변경했다. 댓글·조회·reactionScore는 순위에 사용하지 않는다. |
| 7 | 인기 프로필을 공개·활성·완성·노출 프로필의 유효 공개 팔로워 수로 집계하고 최근 활동, profile ID로 동률을 정한다. 팀 초대 집계는 제거했다. |
| 8 | 인기 작업물 카드와 버튼은 실제 `postId`로 `boards-detail`에 이동하며 제목이나 `workId`를 추측하지 않는다. |
| 9 | `/profiles/:profileId`와 `GET /api/profiles/public/{profileId}`를 추가했다. 공개 API는 공개·활성·완성·노출·정상 USER 조건을 강제하고 이메일을 제거한다. |
| 10 | 인기 프로필 하트에 기존 follow/unfollow API를 연결했다. 자기 자신 버튼을 숨기고 요청 중 중복 클릭을 차단하며 성공 후 랭킹을 다시 조회한다. |
| 11 | WORK 목록 위에 검색 toolbar를 배치하고 category를 WORK로 고정해 조회한다. |
| 12 | WORK 탭에서는 작업물 목록만, FREE 탭에서는 자유게시글 목록만 표시한다. HOME만 두 게시판 요약을 함께 표시한다. |
| 13 | WORK 최신순·좋아요순·조회순·반응순을 backend sort 값으로 전달하며 작업물 종류 필터와 조합한다. |
| 14 | FREE 검색·정렬·세부 분류를 하나의 toolbar에서 조합하고 category를 FREE로 고정한다. |
| 15 | `NOTICE`, `QUESTION`, `INFO`, `REVIEW`, `FREE`를 `board_post.free_category`에 저장한다. 신규 FREE는 필수이며 NOTICE는 backend에서도 관리자만 허용한다. 기존 NULL FREE는 `FREE`로 호환한다. |
| 16 | 일·주·월·년 UI와 기간별 샘플을 제거했다. 인기 화면은 전체 공개 WORK를 좋아요순으로 표시하고 실제 `work_type` DB 필터를 제공한다. |

## 최종 상태 계약

- 탭: `/boards`는 HOME, `?tab=WORK|FREE|POPULAR`은 각 탭을 복원한다.
- 상세 복귀: 상세 query의 `from`으로 HOME/WORK/FREE/POPULAR 복귀 위치를 보존한다.
- 목록 검색: `category`, `keyword`, `sort`, `freeCategory`, `workType`을 명시적으로 전달한다.
- 요청 경합: 게시글과 HOME 요청에 request ID를 사용해 느린 이전 응답이 최신 탭을 덮지 못하게 한다.
- 오류/빈 상태: API 실패, 로딩, 검색 결과 없음, 일반 빈 목록을 샘플 fallback 없이 구분한다.

## 데이터 계약

### 자유게시판 분류

| 코드 | 표시명 |
|---|---|
| `NOTICE` | 공지 |
| `QUESTION` | 질문 |
| `INFO` | 정보 |
| `REVIEW` | 후기 |
| `FREE` | 자유 |

### 작업물 종류

| 코드 | 표시명 |
|---|---|
| `SHORT_FILM` | 단편영화 |
| `FEATURE_FILM` | 장편영화 |
| `MUSIC_VIDEO` | 뮤직비디오 |
| `ADVERTISEMENT` | 광고 |
| `DOCUMENTARY` | 다큐멘터리 |
| `WEB_CONTENT` | 웹 콘텐츠 |
| `OTHER` | 기타 |

기존 `genre`는 작품의 서사 장르이고 `work_type`은 영상 형식이므로 혼용하지 않았다. 두 분류는 `common_code`의 `FREE_POST_CATEGORY`, `WORK_TYPE` 그룹으로 관리한다.

## 스키마 및 migration

- `board_post.free_category varchar(30) NULL`
- `work_item.work_type varchar(30) NULL`
- `team_work_approval_request.work_type varchar(30) NULL`
- `idx_board_post_free_filter`
- `idx_work_type_status`
- 신규 설치: `sql/01_schema.sql`, `sql/02_seed_reference.sql`
- 기존 DB: `sql/10_board_full_integration_schema.sql`

Migration은 `information_schema`로 컬럼과 인덱스를 확인해 재실행할 수 있게 작성했다. 기존 FREE는 `FREE`, 기존 작업물과 승인 요청은 `OTHER`로 보정하고 `board_post.like_count`를 활성 `board_like` 행으로 다시 집계한다.

## 랭킹과 공개 프로필

- 인기 작업물: 공개·게시 중·미삭제 WORK만 조회하고 좋아요, 최신 작성일, post ID 순으로 정렬한다.
- 인기 프로필: 공개·활성·완성·노출 상태의 정상 USER만 대상으로 유효 공개 팔로워를 집계한다.
- `followingByCurrentUser`는 같은 집계 query에서 `EXISTS`로 반환해 N+1을 만들지 않는다.
- 공개 프로필은 편집 action이 없는 `PublicProfileView.vue`에서 표시하고 비공개·삭제·미완성·숨김 프로필은 API 단계에서 거부한다.

## 이미지 계약

- 우선순위: `representativeImageUrl` > `youtubeThumbnailUrl` > `work-city.png`
- 이미지 업로드는 기존 `/api/media/images/work/{postId}` 계약을 유지한다.
- JPEG/PNG/WebP MIME·확장자·signature·5MB·소유권 검증과 교체/삭제 정합성은 기존 `MediaImageService` 테스트를 함께 통과했다.
- 서버 업로드 영상 stream은 이미지 src로 사용하지 않는다.

## 변경 파일

- `backend/src/main/java/com/slate/boards/BoardController.java`
- `backend/src/main/java/com/slate/boards/BoardService.java`
- `backend/src/main/java/com/slate/boards/BoardMapper.java`
- `backend/src/main/java/com/slate/boards/AdminBoardController.java`
- `backend/src/main/java/com/slate/boards/AdminBoardService.java`
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `backend/src/main/java/com/slate/profiles/ProfileController.java`
- `backend/src/main/java/com/slate/profiles/ProfileService.java`
- `backend/src/main/java/com/slate/profiles/ProfileMapper.java`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- `backend/src/main/java/com/slate/security/SecurityConfig.java`
- `backend/src/test/java/com/slate/boards/*FullIntegration*`
- `backend/src/test/java/com/slate/profiles/PublicProfileContractTest.java`
- `frontend/src/views/BoardView.vue`
- `frontend/src/views/PublicProfileView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `sql/01_schema.sql`
- `sql/02_seed_reference.sql`
- `sql/10_board_full_integration_schema.sql`
- frontend/backend/database 기준 문서

## 검증 결과

| 항목 | 결과 |
|---|---|
| backend | `mvn test`: 83개 통과, failures/errors/skipped 0 |
| frontend | `npm run build` 통과. 기존 500kB 초과 chunk 경고만 남음 |
| mapper XML | Board/Profile/Follow mapper `xmllint --noout` 통과 |
| 정적 계약 | category 경계, 검색·분류 인자, NOTICE 권한, 좋아요 recount, 랭킹 SQL, 공개 프로필 접근 조건 테스트 통과 |
| 하드코딩 제거 | 지정 샘플 변수·기간·이미지 import·샘플 제목 검색 결과 0건 |
| diff | `git diff --check` 통과 |
| 실제 MySQL migration | `10_board_full_integration_schema.sql` 2회 적용 완료. 컬럼 3개, 인덱스 2개, 코드 그룹별 5/7개 확인 |
| DB 정합성 | FREE·work type 누락 0건, 좋아요 집계 불일치 0건, 임시 procedure 0건 |
| 실제 최근 작업물 API | `GET /api/boards/posts?category=WORK&sort=latest&limit=4` HTTP 200 및 실제 4건 응답 확인 |
| desktop browser | 1280x720에서 HOME·POPULAR 렌더링, 탭 단독 활성, document/client width 1280 일치, overflow 0건, 콘솔 warning/error 0건 |
| mobile browser | 390x844에서 HOME·WORK·FREE·POPULAR 렌더링, 탭 단독 활성, document/client width 390 일치, overflow 0건, 콘솔 warning/error 0건 |
| 브라우저 발견 수정 | POPULAR section이 `board-editor-shell` 내부에 잘못 중첩돼 미렌더링되던 문제를 형제 section으로 분리하고 재검증 완료 |

## 미수행 검증과 남은 위험

- 실제 로그인 계정 2개를 이용한 좋아요·팔로우 순위 변화와 브라우저 Network query 확인은 수행하지 못했다.
- 비로그인 공개 화면의 반응형과 콘솔은 검증했지만 로그인 후 표시되는 파일 관리·팀 승인 action 및 follow mutation은 이번 browser smoke 범위에서 제외했다.
- 기존 빌드에는 500kB를 넘는 JS chunk 경고가 있으며 이번 작업에서 새 라이브러리는 추가하지 않았다.

## 참조 경로

- `docu/prompt/board_full_integration_fixer_prompt.md`
- `docu/user_temp/What_to_do.md`
- `Agent.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
