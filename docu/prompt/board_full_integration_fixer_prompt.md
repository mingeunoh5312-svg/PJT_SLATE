# 게시판 검색·분류·랭킹·팔로우 통합 수정 프롬프트

```text
Slate 게시판의 검색, 탭, 하드코딩 데이터, 작업물 이미지, 도구 버튼, 인기 순위, 상세 이동, 프로필 팔로우, 자유게시판 분류와 정렬을 한 번의 연속 작업으로 직접 수정하세요. 분석이나 일부 화면 수정에서 멈추지 말고 DB migration, backend, frontend, 테스트, 실제 브라우저 검증, 작업 로그까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

원본 요구사항:
- `docu/user_temp/What_to_do.md`

이 프롬프트에서 “홈”은 별도 사이트 홈(`/`)이 아니라 `BoardView.vue`의 게시판 내부 `HOME` 탭을 뜻합니다. 원본 요구사항과 실제 라우트/UI를 다시 확인한 결과 다른 의미가 명백할 때만 범위를 조정하고 작업 로그에 근거를 기록하세요.

## 먼저 확인

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `frontend/src/views/BoardView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/boards/**`
- `backend/src/main/java/com/slate/profiles/**`
- `backend/src/main/java/com/slate/follows/**` 또는 실제 follow 구현 위치
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `backend/src/main/resources/mappers/ProfileMapper.xml`
- follow 관련 mapper XML
- `sql/01_schema.sql`
- 관련 migration SQL, 테스트, 작업 로그

시작 시 `git status --short`와 관련 파일 diff를 확인하세요. 현재 작업 트리에는 사용자와 다른 작업자의 미완료 변경이 있을 수 있으며, `BoardView.vue`, `BoardMapper.xml`, `BoardService.java`, `slate.css`, schema에 작업물 대표 이미지 관련 변경이 이미 들어 있을 수 있습니다. 기존 변경을 되돌리거나 과거 prototype 파일로 덮어쓰지 말고 현재 구현을 이어서 완성하세요.

## 확인된 현재 문제

1. 게시판 홈 검색은 사실상 작업물 검색으로만 동작하며 자유게시판 필터가 없다.
2. `sampleWorks`, `sampleFreePosts`, `popularPeriodWorks`, `popularProfiles`와 여러 샘플 이미지가 실제 DB 데이터처럼 노출된다.
3. `activeTab` 기본값이 `WORK`인데 HOME 활성 조건은 `!activeBoardPanel`이어서 HOME과 작업물이 동시에 눌린 것처럼 보일 수 있다.
4. 작업물 대표 이미지 경로와 업로드 상태가 일부 구현되어 있을 수 있으나, 모든 생성·수정·조회·랭킹 응답과 UI에서 일관되게 연결됐는지 검증되지 않았다.
5. 내 업로드 파일 관리와 팀 작업물 승인 기능이 상단 action이 아닌 별도 quick card 영역에 있다.
6. 인기 작업물 SQL은 좋아요 수가 아니라 좋아요·댓글·조회수를 섞은 `reactionScore`를 사용한다.
7. 인기 프로필 SQL은 팔로워 수가 아니라 최근 팀 초대 수를 사용한다.
8. 실제 인기 작업물은 `postId`가 있을 때만 상세 이동이 가능하고 샘플 항목은 상세가 없다.
9. 인기 프로필용 공개 상세 route와 연결이 없거나 현재 화면에서 사용할 수 있는 계약이 없다.
10. 인기 프로필에 실제 follow API와 연결된 하트 버튼이 없다.
11. 검색과 정렬 UI가 작업물/자유게시판 목록 상단에 일관되게 배치되지 않았다.
12. WORK 탭에서도 HOME용 자유게시판/추천 섹션이 섞여 보일 수 있다.
13. backend 목록 API는 `latest`, `likes`, `views`, `reaction` 정렬을 지원하지만 frontend 연결과 표시가 불완전하다.
14. 자유게시판 검색과 상단 정렬이 없다.
15. 자유게시판의 `공지`, `질문`, `정보`, `후기`, `자유` 세부 분류를 저장할 DB/API 필드가 없다.
16. 인기 화면의 일·주·월·년 선택과 샘플 기간별 데이터는 요구사항과 다르며, 좋아요 순 및 작업물 종류 필터가 필요하다.

## 최종 화면 계약

### 게시판 상단 공통 영역

- 게시판 탭은 `홈`, `작업물`, `자유`, `인기` 중 실제 현재 화면 하나만 active 상태여야 한다.
- HOME 진입 시 작업물 탭이 동시에 active가 되지 않는다.
- 오른쪽 상단 action을 동일 높이와 시각 규격으로 정렬한다.
  - 글쓰기
  - 작업물 올리기
  - 내 업로드 파일 관리
  - 팀 작업물 승인
- 로그인이나 팀 권한이 필요한 action은 기존 인증/권한 흐름을 유지한다. 권한이 없는 사용자에게 작동하지 않는 버튼을 실제 기능처럼 보여주지 않는다.
- 기존 quick card가 상단 action과 중복되면 제거한다. 기능 route와 handler 자체를 삭제하지 않는다.
- desktop 및 390x844에서 action이 겹치거나 가로 overflow하지 않도록 wrap 또는 모바일 배치를 제공한다.

### 1. 게시판 HOME 검색

- HOME 상단에 검색어 입력과 게시판 범위 필터를 둔다.
- 범위 필터는 최소 `작업물`, `자유게시판`을 제공한다.
- 검색 실행 시 선택 범위의 실제 DB 게시글만 조회한다.
- 제목과 본문은 공통 검색 대상이다. 작업물은 기존 작품 제목, 설명, YouTube 제목/채널, 작성자, 팀 이름 등 현재 backend가 지원하는 검색 대상을 보존한다.
- 검색 결과가 없으면 샘플 카드로 대체하지 않고 명확한 빈 상태를 표시한다.
- 검색어 앞뒤 공백을 정리하고 같은 검색의 불필요한 중복 요청을 막는다.
- 검색 결과 클릭 시 실제 게시글 상세로 이동한다.
- HOME 검색과 WORK/FREE 탭의 검색 상태가 섞여 잘못된 category 요청을 보내지 않도록 route/tab 전환 정책을 명확히 한다.

### 2. 하드코딩 제거와 DB 연동

- `BoardView.vue`를 전수 검색해 아래 실제 데이터처럼 보이는 샘플을 제거한다.
  - `sampleWorks`
  - `sampleFreePosts`
  - `popularWorks`
  - `popularPeriodWorks`
  - `popularProfiles`
  - 샘플 제목, 작성자, 좋아요, 댓글, 조회수, 날짜, 순위, 프로필, 이미지 fallback
- API 응답이 비었거나 실패했을 때 샘플 데이터로 채우지 않는다.
- 빈 배열은 정상적인 빈 결과로 취급하고 로딩, 오류, 빈 상태를 구분한다.
- 실제 데이터에 이미지가 없으면 공통 default 작업물/프로필 이미지를 사용한다. 서로 다른 샘플 이미지를 index로 순환 배정하지 않는다.
- import가 불필요해진 샘플 assets는 코드 import에서 제거하되 다른 화면이 사용하는 실제 파일은 삭제하지 않는다.

### 3. 탭 선택 상태

- HOME, WORK, FREE, POPULAR을 하나의 명확한 상태 모델로 관리한다.
- tab active CSS를 `activeTab`과 `activeBoardPanel`의 서로 모순되는 조건으로 계산하지 않는다.
- 브라우저 뒤로 가기, 상세에서 목록 복귀, 직접 `/boards` 접근, 작성/수정 완료 후에도 올바른 탭과 패널이 표시되어야 한다.
- 탭 전환 시 이전 검색 결과나 상세 데이터가 현재 탭의 최신 결과처럼 남지 않게 한다.

### 4. 추천 작업물 이미지

- HOME 추천 작업물, WORK 목록, 인기 작업물에서 같은 이미지 우선순위를 사용한다.
- 권장 우선순위: 사용자가 직접 업로드한 대표 이미지 > YouTube 썸네일 > 공통 default 작업물 이미지.
- 현재 `representativeImageUrl`, `youtubeThumbnailUrl` 및 작업물 이미지 upload/delete 구현을 조사하고 누락된 생성·수정·상세·목록·랭킹 경로를 완성한다.
- 브라우저 object URL이나 base64를 영구 저장으로 사용하지 않는다.
- 서버 업로드 영상 파일 자체를 `<img>`의 src로 사용하지 않는다.
- 직접 이미지 업로드는 MIME, 확장자, 실제 signature, 크기, 소유권을 backend에서 검증한다.
- 이미지 교체/삭제 시 DB와 실제 파일이 불일치하지 않게 하고, 없는 이미지는 default로 복귀한다.
- 깨진 이미지 URL도 무한 오류 없이 default 이미지로 전환한다.

### 5. 상단 관리 버튼 이동

- `내 업로드 파일 관리`, `팀 작업물 승인` action을 `글쓰기`, `작업물 올리기` 옆으로 이동한다.
- 네 action의 높이, padding, 글꼴, icon 정렬을 동일 계열로 맞춘다. primary/outline 강조 차이는 유지할 수 있다.
- 내 업로드 파일 관리는 기존 `/profile/files` route를 유지한다.
- 팀 작업물 승인은 기존 승인 panel과 권한 확인을 유지한다.
- 이동 후 기존 quick card 또는 중복 진입점 때문에 같은 기능이 두 번 노출되지 않게 한다.

### 6. 인기 작업물 순위

- 인기 작업물은 공개·게시 중인 WORK 게시글만 대상으로 한다.
- 순위 기준은 `like_count DESC`이다.
- 동률 기준은 최신 작성일, 그다음 `post_id DESC`처럼 결정적으로 정의한다.
- 댓글 수, 조회수, `reactionScore`를 순위에 섞지 않는다.
- 집계된 `board_post.like_count`가 실제 활성 `board_like` 행과 일치하는지 기존 recount 로직과 테스트로 확인한다.
- frontend 순위, 좋아요 숫자, 정렬 순서가 backend 응답 그대로 표시되어야 하며 frontend에서 재계산하거나 샘플 수치로 대체하지 않는다.

### 7. 인기 프로필 순위

- 공개·활성·완성된 프로필만 대상으로 한다.
- 순위는 해당 프로필의 활성 팔로워 수 기준 내림차순으로 정한다.
- 동률 기준은 최근 활동일과 profile ID 등으로 결정적으로 정의한다.
- 팀 초대 수 `invitationCount`를 인기 프로필 기준으로 사용하지 않는다.
- follow 테이블의 실제 활성 관계 조건과 차단/삭제/비공개 정책을 기존 follow mapper/service에서 재사용한다.
- backend 응답에 `profileId`, 표시 이름, 역할 또는 소개, `followerCount`, 현재 로그인 사용자의 `followingByCurrentUser`를 N+1 없이 제공한다.
- frontend는 팔로워 수와 순위를 backend 응답 그대로 표시한다.

### 8. 인기 작업물 상세 이동

- 인기 작업물의 제목과 카드에 접근 가능한 상세 링크 또는 버튼을 제공한다.
- 실제 `postId`를 사용해 `{ name: 'boards-detail', params: { postId } }`로 이동한다.
- `workId`를 `postId`로 오인하거나 제목으로 게시글을 찾지 않는다.
- 키보드로도 진입 가능하게 하고 카드 내부 좋아요 등 다른 action과 클릭 충돌을 막는다.

### 9. 인기 프로필 상세 이동

- 인기 프로필 이름 또는 카드 클릭 시 해당 공개 프로필 상세로 이동한다.
- 기존 Matching 상세 UI/API를 재사용할 수 있는지 먼저 확인한다.
- 공개 프로필 route가 없다면 `profileId`를 받는 명시적인 route와 조회 화면을 추가한다.
- 내 정보 `/profile` 편집 화면을 타인의 프로필 상세처럼 재사용해 편집 action이 노출되지 않게 한다.
- 비공개, 삭제, 미완성 프로필은 직접 URL 접근에서도 노출하지 않는다.
- frontend가 이름으로 프로필을 추측하지 않고 backend가 제공한 `profileId`를 사용한다.

### 10. 인기 프로필 하트 팔로우

- 각 인기 프로필 옆에 하트 모양 follow 버튼을 둔다.
- 팔로우하지 않은 상태와 팔로우 중인 상태를 `aria-label`, `aria-pressed`, 색상 또는 채움으로 구분한다.
- 기존 follow/unfollow API를 재사용하고 별도 중복 follow 시스템을 만들지 않는다.
- 자기 자신에게는 팔로우 버튼을 표시하지 않는다.
- 비로그인 클릭은 기존 로그인 유도 흐름을 사용한다.
- 요청 중 중복 클릭을 막고 실패 시 이전 상태로 복원하며 오류를 표시한다.
- 성공 후 해당 카드의 `followerCount`, 하트 상태, 인기 프로필 목록 순서를 backend 기준으로 다시 동기화한다.

### 11. WORK 검색 위치

- WORK 탭 상단, 결과 목록보다 앞에 검색 UI를 배치한다.
- HOME 검색과 동일한 입력·제출·초기화 패턴과 시각 규격을 사용한다.
- WORK category를 backend에 명시해 자유게시판 결과가 섞이지 않게 한다.
- 검색 중, 오류, 검색 결과 없음, 일반 빈 목록을 구분한다.

### 12. WORK 화면 구성 분리

- WORK 탭에서는 작업물 목록과 추천 작업물만 표시한다.
- 자유게시판 preview/list는 WORK 탭에서 숨긴다.
- 인기 작업물과 인기 프로필 side section은 유지할 수 있다.
- HOME은 작업물과 자유게시판 요약을 함께 표시할 수 있으나 모두 실제 DB 데이터를 사용한다.
- FREE 탭에는 자유게시판만 표시한다.

### 13. WORK 정렬

- WORK 검색 옆 또는 같은 상단 toolbar에 정렬 선택을 둔다.
- 최소 최신순, 좋아요순, 조회순을 제공한다.
- `reaction` 정렬을 유지한다면 사용자에게 의미가 분명한 한국어 라벨을 제공한다. 인기 순위 자체는 반드시 좋아요순 규칙을 따른다.
- 정렬 선택 시 backend에 지원되는 sort code를 전달하고 frontend 배열만 임의 정렬하지 않는다.
- 검색어, 정렬, category 변경 시 일관되게 재조회한다.

### 14. FREE 검색과 정렬

- FREE 탭 상단에 WORK/HOME과 같은 검색 UI를 제공한다.
- category는 항상 `FREE`로 요청한다.
- 최신순, 좋아요순, 조회순 정렬을 제공한다.
- 세부 분류 filter와 검색·정렬을 함께 사용할 수 있어야 한다.
- 검색 결과가 없을 때 하드코딩 자유게시글을 표시하지 않는다.

### 15. 자유게시판 5개 세부 분류

- 자유게시판 세부 분류는 다음 5개 고정 코드와 한국어 라벨로 관리한다.
  - `NOTICE`: 공지
  - `QUESTION`: 질문
  - `INFO`: 정보
  - `REVIEW`: 후기
  - `FREE`: 자유
- `board_post.category`의 `WORK`/`FREE`와 세부 분류를 혼용하지 않는다. nullable `board_type`, `free_category` 등 저장소 규칙에 맞는 별도 컬럼을 사용한다.
- 신규 FREE 작성 시 세부 분류를 필수로 선택하게 한다.
- WORK 게시글에는 자유게시판 세부 분류를 저장하지 않는다.
- 수정 진입 시 기존 값을 복원하고 저장 후 목록·상세에 한국어 라벨을 표시한다.
- FREE 목록 상단에서 전체 및 5개 분류 filter를 제공한다.
- `NOTICE` 작성 권한은 현재 관리자 정책을 확인해 관리자만 허용하는 방향을 우선한다. 일반 사용자가 공지를 작성할 수 있게 하지 말고 backend에서도 검증한다.
- 기존 FREE 데이터는 migration 후 `FREE` 분류로 호환되게 처리하되, 데이터 의미를 추측해 다른 분류로 backfill하지 않는다.
- 관리자 게시글 조회·수정 API와 mapper에도 새 필드가 누락되지 않게 한다.

### 16. 인기 화면 개편

- 일·주·월·년 기간 탭과 `popularPeriods`, `popularPeriodWorks` 하드코딩을 제거한다.
- 인기 작업물은 전체 공개 WORK 게시글을 좋아요순으로 표시한다.
- 인기 화면 상단에 작업물 종류별 filter를 제공한다.
- 먼저 현재 schema/reference data에 작품 종류 또는 장르 계약이 있는지 확인한다.
  - 재사용 가능한 실제 분류가 있으면 해당 ID/code로 필터링한다.
  - 작품 종류 계약이 없다면 제목/tag 문자열을 추측해 분류하지 않는다. 별도 nullable controlled code와 reference/API/UI를 추가하고 기존 데이터는 `기타` 또는 미분류로 호환한다.
- 작업물 종류 코드와 라벨은 기존 제품 문서·공통 코드·장르 데이터를 우선 사용하고 작업 로그에 최종 계약을 기록한다.
- filter는 backend query에서 적용하고 frontend 샘플 배열만 필터링하지 않는다.
- 인기 프로필 section은 팔로워 수 기준으로 계속 표시한다.

## DB 및 migration

- `sql/01_schema.sql`에 신규 설치용 최종 schema를 반영한다.
- 기존 DB에 적용 가능한 별도 멱등 migration SQL을 `sql/`에 추가한다.
- 최소 자유게시판 세부 분류 컬럼과 필요한 작업물 종류 계약을 반영한다.
- 대표 이미지 관련 schema가 현재 작업 트리에 이미 추가됐다면 중복 컬럼/FK를 만들지 말고 기존 계약을 완성한다.
- 검색·좋아요 순위·분류 filter에 필요한 인덱스를 실제 query와 함께 검토한다.
- migration 재실행 시 중복 컬럼, 인덱스, FK 오류가 없어야 한다.
- 기존 FREE 게시글은 읽기와 수정이 가능해야 하며 샘플 데이터를 DB에 seed하지 않는다.

## Backend 요구사항

- 게시글 목록 API는 category, keyword, sort, freeCategory, workType 등 실제 구현한 filter를 명시적으로 검증한다.
- 허용하지 않는 category/sort/filter는 조용히 다른 값으로 처리하지 말고 기존 예외 정책에 맞게 거부한다.
- HOME 통합 검색을 단일 endpoint로 만들거나 기존 endpoint를 두 번 호출할 수 있으나, 결과 category가 섞이지 않고 권한/공개 범위가 유지되어야 한다.
- 인기 작업물은 좋아요순 전용 mapper query로 명확히 구현한다.
- 인기 프로필은 follow 집계 query로 구현하고 N+1을 만들지 않는다.
- 현재 사용자별 follow 상태가 필요한 query는 익명 사용자도 정상 동작하게 한다.
- 공개 프로필 상세 API는 visibility/status/completed/activity 조건을 backend에서 강제한다.
- 이미지 업로드는 기존 작업물 대표 이미지 구현과 보안 정책을 재사용한다.
- 자유게시판 세부 분류를 create/update/list/detail/admin 응답 전체에서 보존한다.
- 좋아요 toggle 후 `like_count` 재집계와 인기 순위 반영을 테스트한다.

## Frontend 요구사항

- `BoardView.vue`가 이미 큰 파일이므로 실제 복잡도와 중복을 줄이는 경우에만 검색 toolbar, 인기 프로필 카드 등을 작은 component로 분리한다.
- API 데이터의 0, false, 빈 문자열, 빈 배열을 `|| 샘플값`으로 덮지 않는다.
- route 이동은 문자열 URL보다 기존 named route를 우선한다.
- 요청 경합 시 이전 tab/search 응답이 현재 화면을 덮지 않게 request ID 또는 동등한 방식을 적용한다.
- loading, error, empty를 구분하고 API 오류 시 샘플 UI로 fallback하지 않는다.
- 실제 이미지가 없으면 공통 default 이미지를 사용하되 사용자별 임의 샘플 이미지를 배정하지 않는다.
- 검색 input, sort select, category filter, 하트 버튼에 접근 가능한 label과 focus 상태를 제공한다.

## 필수 테스트

### Backend

1. WORK/FREE keyword 검색이 category를 넘지 않고 제목·본문에서 동작.
2. 최신순·좋아요순·조회순 정렬 결과와 동률 순서 검증.
3. FREE 5개 분류 생성·수정·목록·상세·filter 왕복 검증.
4. 일반 사용자의 NOTICE 작성 거부와 관리자의 작성 허용.
5. 기존 분류 없는 FREE 데이터가 `FREE`로 호환.
6. 인기 작업물이 활성 좋아요 수 내림차순이며 댓글·조회수 변화로 순위가 뒤집히지 않음.
7. 좋아요 toggle 후 count와 인기 순위 갱신.
8. 인기 프로필이 활성 팔로워 수 내림차순이며 팀 초대 수와 무관함.
9. 자기 자신 follow 금지, 중복 follow 방지, follow/unfollow 후 count와 상태 갱신.
10. 공개 프로필 상세의 공개·삭제·미완성 접근 제한.
11. 작업물 종류 filter가 실제 DB 조건으로 적용됨.
12. 대표 이미지/YouTube/default URL 계약과 이미지 권한·파일 검증.
13. migration 신규 적용과 재실행 성공.

### Frontend 및 브라우저

1. `/boards` 최초 진입에서 HOME만 active.
2. HOME에서 WORK/FREE 범위 검색과 결과 상세 이동.
3. 검색 결과 없음에 샘플 게시글 미노출.
4. WORK 탭에는 자유게시판 section이 없고 검색·정렬이 상단에 표시.
5. FREE 탭에 검색·정렬·전체/5개 분류 filter 표시 및 조합 동작.
6. 글쓰기/수정에서 FREE 분류 저장·복원·상세 표시.
7. 대표 이미지 > YouTube 썸네일 > default 이미지 우선순위 확인.
8. 상단 네 action의 이동, 크기, 권한별 동작 확인.
9. 인기 작업물이 실제 좋아요 수 순서와 일치하고 제목 클릭 시 상세 이동.
10. 인기 프로필이 팔로워 수 순서와 일치하고 이름 클릭 시 공개 상세 이동.
11. 하트 follow/unfollow, 비로그인 유도, 자기 자신 버튼 미노출.
12. 인기 화면에 일·주·월·년 항목이 없고 작업물 종류 filter가 동작.
13. 뒤로 가기와 직접 URL 접근 후 탭/검색/상세 상태가 깨지지 않음.
14. desktop 및 390x844에서 toolbar/action/card overflow와 console error 없음.

## 실행 및 검증

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

- mapper XML 문법 검사
- migration SQL 신규 적용 및 재실행 검사
- 실제 로그인 계정 2개 이상으로 좋아요와 follow 순위 변화 확인
- 브라우저 Network에서 category, keyword, sort, freeCategory/workType, profileId 요청 확인
- DB에서 `board_like`, follow 관계, 집계 count, 자유게시판 분류 값 확인
- `git diff --check`
- 기존 사용자 변경 때문에 실패한 검증은 원인을 분리해 기록하고 기존 변경을 되돌려 통과시키지 않는다.

## 금지사항

- 하드코딩 샘플을 API 실패 fallback으로 유지 금지
- 실제 DB 데이터가 없을 때 샘플 게시글, 프로필, 좋아요 수, 순위 표시 금지
- frontend 배열만 정렬하고 인기 순위가 구현됐다고 처리 금지
- 팀 초대 수를 팔로워 수처럼 표시 금지
- 제목이나 사용자 이름으로 상세 ID를 추측 금지
- 자유게시판 분류를 제목 prefix나 CSS label에만 저장 금지
- 작품 종류를 제목/tag 문자열 포함 여부로 임의 분류 금지
- 다른 사용자의 비공개 프로필·게시글 노출 금지
- 권한 검사 없는 공지 작성, 이미지 업로드, 게시글 수정 금지
- 기존 작업물 승인, 영상 업로드, YouTube preview, 리뷰, 신고, 관리자 moderation 흐름 삭제 금지
- 실제 API key, DB 비밀번호, JWT, `.env` 값 출력·문서화 금지
- 관련 없는 리팩터링, 사용자 변경 되돌리기, 새 라이브러리 설치, commit/push 금지
- `../prototype*` 수정 금지. 비교가 필요하면 읽기 전용으로만 사용

## 완료 보고

`docu/work_logs/YYYY-MM-DD_fixer_board_full_integration.md`에 다음을 기록하세요.

- 16개 요구사항별 수정 결과
- 제거한 하드코딩 목록
- tab/search/sort/filter 최종 상태 계약
- 자유게시판 세부 분류와 작업물 종류 데이터 계약
- schema 및 migration 변경
- 인기 작업물 좋아요 순위 SQL과 동률 규칙
- 인기 프로필 팔로워 순위 SQL과 follow 상태 계약
- 공개 프로필 route/API와 접근 제한
- 작업물 이미지 우선순위와 업로드 보안
- 변경 파일 목록
- backend test, frontend build, SQL/API/브라우저 검증 결과
- 미수행 검증과 남은 위험
```
