# 게시판 검색 UI·장르 필터·기간별 인기 순위 후속 수정 프롬프트

```text
Slate 게시판의 검색 UI, 검색 결과 이동, 최신 작업물, 이미지 fallback, 작품 유형·장르 필터, 작업물 action, 인기 순위와 자유게시판 레이아웃을 직접 수정하세요. 설명이나 CSS 일부 수정에서 멈추지 말고 필요한 DB, backend, frontend, 테스트, 실제 브라우저 검증과 작업 로그까지 완료하세요.

작업 루트:
- `/Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate`

원본 요구사항:
- `docu/user_temp/What_to_do_2.md`

참고 이미지:
- 잘못된 기존 게시판 범위 UI: `/Users/mingeunoh/Desktop/스크린샷 2026-06-22 오후 8.48.30.png`
- 개선이 필요한 기존 자유게시판 UI: `/Users/mingeunoh/Desktop/스크린샷 2026-06-22 오후 9.09.00.png`
- 게시판 범위 탭의 목표 시각 스타일: `/Users/mingeunoh/Desktop/스크린샷 2026-06-22 오후 9.14.43.png`

목표 시각 스타일 이미지는 `추천 팀/저장한 팀`이라는 문구를 복사하라는 뜻이 아닙니다. 파란 활성 글자와 하단 indicator를 가진 간결한 가로 탭 형태만 참고하고, 실제 탭 라벨은 `작업물`, `자유 게시판`을 사용하세요.

## 먼저 확인

- `Agent.md`
- `docu/README.md`
- `docu/00_common/reference_policy.md`
- `docu/05_backend/backend_baseline.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/07_database/database_baseline.md`
- `docu/prompt/board_full_integration_fixer_prompt.md`
- 해당 통합 작업 이후 작성된 최신 board 관련 work log
- `frontend/src/views/BoardView.vue`
- board 관련 component가 분리되어 있다면 해당 파일
- `frontend/src/services/api.js`
- `frontend/src/router/index.js`
- `frontend/src/styles/slate.css`
- `backend/src/main/java/com/slate/boards/**`
- `backend/src/main/resources/mappers/BoardMapper.xml`
- `sql/01_schema.sql`
- board 관련 migration SQL과 테스트

시작 시 `git status --short`와 관련 파일 diff를 확인하세요. 현재 작업 트리에는 사용자 또는 다른 수정자의 미완료 변경이 있을 수 있습니다. 기존 게시판 DB 연동, 하드코딩 제거, 대표 이미지 업로드, 자유게시판 분류, 좋아요/팔로워 순위, 공개 프로필 이동, follow 기능을 되돌리거나 과거 prototype 코드로 덮어쓰지 마세요.

## 확정 사항

- 1번의 첫 이미지는 잘못된 현재 UI이다.
- 게시판 범위는 세 번째 이미지와 같은 가로 탭 형태로 개선한다.
- 작업물 게시판에서는 `글쓰기` 버튼을 삭제한다.
- `작업물 올리기` 버튼은 유지한다.
- 인기 작업물은 주별, 월별, 전체 범위로 제공한다.
- 순위 기준은 각 기간 내 공개 작업물의 좋아요 수 내림차순이다.

## 1. 게시판 범위 UI

- 기존의 과도하게 큰 select/card 형태 `게시판 범위` UI를 제거한다.
- 검색 영역 상단 또는 내부에 `작업물`, `자유 게시판` 가로 탭을 배치한다.
- 선택된 탭은 파란색 글자, 굵기, 하단 파란 indicator로 표시한다.
- 선택되지 않은 탭은 중립색으로 표시하되 충분한 대비를 유지한다.
- tab semantics 또는 `role="tablist"`, `role="tab"`, `aria-selected` 등 접근 가능한 구조를 사용한다.
- 키보드 focus가 보이고 tab 클릭 영역이 글자에만 지나치게 좁지 않게 한다.
- 기존 스크린샷처럼 select와 초기화 버튼이 큰 카드 높이를 차지하지 않게 한다.
- desktop과 390x844에서 좌우 여백, indicator 폭, 줄바꿈을 확인한다.

## 2. 추천 작업물을 최신 작업물로 변경

- 게시판 HOME의 `추천 작업물` 제목을 `최신 작업물`로 변경한다.
- 문구만 바꾸지 말고 backend에 `sort=latest` 또는 동등한 최신순 조건을 요청한다.
- 공개·게시 중인 WORK 게시글만 대상으로 `created_at DESC`, 동률 시 `post_id DESC`로 정렬한다.
- 실제 DB 결과만 표시하고 데이터가 없을 때 샘플 작업물을 출력하지 않는다.

## 3. 검색 시 결과 페이지로 이동

- HOME에서 검색을 실행하면 같은 화면 안의 임시 panel만 여는 방식이 아니라 명시적인 검색 결과 route로 이동한다.
- 권장 route 계약:
  - path: `/boards/search`
  - name: `boards-search`
  - query: `scope=WORK|FREE`, `keyword`, 필요 시 `sort`, `genreId`, `workType`, `freeCategory`
- 기존 router 구조에 더 자연스러운 동등 계약이 있다면 그 방식을 사용할 수 있으나 URL 새로고침, 링크 공유, 뒤로 가기가 정상이어야 한다.
- 검색 결과 페이지 상단에는 검색어, 범위 탭, 해당 범위의 filter와 sort를 유지한다.
- URL query를 단일 source of truth로 사용해 새로고침 후 동일 결과를 복원한다.
- 빈 검색어는 결과 페이지로 이동하지 않거나 전체 목록 의미를 명확히 정의한다.
- 검색 결과 클릭 시 실제 게시글 상세 route로 이동한다.
- 이전 요청이 늦게 도착해 현재 검색 결과를 덮지 않게 한다.

## 4. 초기화 버튼 제거

- 검색 영역의 사용자 노출 `초기화` 버튼을 삭제한다.
- 버튼 삭제 후 검색 input과 검색 button, filter의 높이와 간격을 다시 맞춘다.
- 내부 상태 초기화 helper가 route 전환이나 새 검색에 필요하면 유지할 수 있으나 화면에 별도 초기화 action을 노출하지 않는다.
- 검색어는 input에서 직접 지운 뒤 검색하거나 범위 탭 이동으로 변경할 수 있어야 한다.

## 5. 최신/추천 작업물 이미지

- 작업물 카드의 이미지 우선순위는 다음과 같이 유지한다.
  1. 사용자가 직접 업로드한 대표 이미지
  2. 검증된 YouTube metadata의 썸네일
  3. 작업물 장르에 대응하는 default 이미지
  4. 장르도 없거나 해당 asset이 아직 없으면 공통 neutral 작업물 default
- YouTube URL 문자열만 조작해 thumbnail URL을 추측하지 말고 기존 metadata 응답의 `youtubeThumbnailUrl`을 사용한다.
- 장르별 default 이미지는 `genreId` 또는 안정적인 genre code 기반 mapping으로 설계한다. 한국어 표시 이름 문자열 비교에 의존하지 않는다.
- 장르별 이미지 asset은 추후 제작 예정이므로 존재하지 않는 이미지를 임의 생성하거나 샘플 작품 이미지를 장르 이미지처럼 사용하지 않는다.
- 지금 단계에서는 장르별 mapping을 쉽게 추가할 수 있는 helper/config 구조와 공통 neutral default를 구현하고, 준비된 asset만 안전하게 연결한다.
- 복수 장르라면 대표 장르 선택 규칙을 명시한다. 기존 sort order가 있으면 첫 번째 장르를 우선한다.
- backend 목록/랭킹 응답에 대표 장르 ID/code/name이 없다면 N+1 없이 제공한다.
- 깨진 이미지 URL은 무한 오류 없이 다음 fallback으로 전환한다.

## 6. 장르 필터 추가

- 현재 작품 분류 filter와 별도로 `장르` filter를 추가한다.
- 장르 목록은 reference API 또는 DB의 실제 genre 데이터를 사용하고 frontend에 배열로 하드코딩하지 않는다.
- 전체 장르 option을 제공한다.
- HOME 검색 결과, WORK 목록, 인기 작업물에서 화면 목적에 맞게 장르 filter를 연결한다.
- 장르 filter는 frontend 배열만 거르는 방식이 아니라 backend query 조건으로 적용한다.
- 작업물과 장르 사이의 영구 저장 관계가 아직 없다면 기존 team genre를 작품 genre처럼 추측해서 사용하지 않는다. `work_item`과 genre의 다대다 관계 또는 저장소 규칙에 맞는 명시적 계약을 추가한다.
- 작업물 등록·수정에서 장르를 선택하고 저장·복원할 수 있어야 하며 목록/상세/검색/인기 응답에서도 같은 장르가 반환되어야 한다.
- 기존 작품은 장르가 없어도 정상 조회되고 `전체`에서는 표시되어야 한다.

## 7. `작업물 종류` 명칭 변경

- 사용자 노출 라벨 `작업물 종류`를 `작품 유형`으로 변경한다.
- 내부 API field/code 이름까지 불필요하게 변경하지 않는다.
- 검색 결과, WORK 탭, 인기 탭 등 같은 filter가 노출되는 모든 화면에서 라벨을 통일한다.
- 자유게시판의 `세부 분류`와 혼동되지 않게 한다.

## 8. 작업물 게시판 글쓰기 버튼 삭제

- WORK 탭과 WORK 검색 결과에서는 일반 `글쓰기` 버튼을 표시하지 않는다.
- `작업물 올리기` 버튼은 유지하고 기존 WORK 등록 route로 이동한다.
- FREE 탭과 FREE 검색 결과에서는 자유게시판 글쓰기 action을 유지한다.
- HOME에서는 현재 UX를 검토해 두 action의 의미가 중복되지 않도록 하되, 자유게시글 작성 진입은 사라지지 않게 한다.
- frontend 표시만 숨기는 요구사항이며 기존 backend 게시글 작성 권한/API를 삭제하지 않는다.

## 9. 인기 게시판 좌우 배치와 표시 개수

- POPULAR 화면에서 `인기 작업물`과 `인기 프로필`을 desktop에서 좌우 2열로 배치한다.
- 인기 작업물은 정확히 상위 5개, 인기 프로필은 정확히 상위 10개까지 요청·표시한다.
- frontend에서 더 많은 결과를 받은 뒤 임의로 샘플과 섞지 말고 API limit을 각각 5와 10으로 전달한다.
- 인기 작업물 영역과 프로필 영역의 heading, 순위 숫자, 행 높이, 빈 상태를 정돈한다.
- mobile에서는 읽기 편하도록 1열 세로 배치로 전환한다.
- 긴 작품명/프로필명 때문에 옆 column이 밀리지 않게 한다.
- 기존 작품 상세 이동, 공개 프로필 상세 이동, 하트 follow 기능을 유지한다.

## 10. 주별·월별·전체 인기 작업물

- backend에 세 기간을 명시적으로 구현한다.
  - `WEEKLY`: 현재 시각 기준 최근 7일
  - `MONTHLY`: 현재 시각 기준 최근 30일
  - `ALL`: 기간 제한 없음
- 모든 기간은 공개·게시 중인 WORK 게시글만 대상으로 한다.
- 정렬은 `like_count DESC`, `created_at DESC`, `post_id DESC` 순서로 고정한다.
- 댓글, 조회수, `reactionScore`를 순위에 섞지 않는다.
- 기존 `POPULAR_WORK`, `MONTHLY_WORK` 등 ranking type이 있다면 호환성을 검토해 `WEEKLY_WORK`, `MONTHLY_WORK`, `POPULAR_WORK` 또는 더 명확한 한 계약으로 통일한다.
- 잘못된 type은 전체로 조용히 fallback하지 말고 검증한다.
- frontend 인기 작업물 heading 근처에 `주별`, `월별`, `전체` 탭을 제공한다.
- 기간 선택 시 인기 작업물 5개만 재조회하고 인기 프로필 목록과 follow 상태를 불필요하게 초기화하지 않는다.
- 선택 기간은 URL query 또는 안정적인 화면 상태로 유지해 뒤로 가기와 새로고침을 검증한다.
- 기간별 데이터가 없으면 샘플 순위를 출력하지 않는다.

## 11. 자유게시판 UI 개선

- 두 번째 참고 이미지는 목표가 아니라 현재 잘못된 UI이다.
- 각 자유게시글이 화면 전체 폭의 거대한 빈 카드와 긴 `보기` 버튼으로 늘어나지 않게 한다.
- 목록형 레이아웃을 우선 적용한다.
  - 세부 분류 badge
  - 제목
  - 짧은 본문 preview
  - 작성자와 작성일
  - 좋아요, 댓글, 조회수
  - 행 전체 또는 제목의 상세 링크
- 제목과 본문이 왼쪽 좁은 영역에서 글자 단위로 부자연스럽게 줄바꿈되지 않게 `min-width`, grid column, line clamp를 조정한다.
- 별도의 가로 전체 `보기` 버튼을 제거하고 제목/행 클릭으로 상세 이동하게 한다.
- 공지, 질문, 정보, 후기, 자유 badge는 기존 DB code를 한국어 label로 표시한다.
- loading/error/empty 상태가 카드 높이를 과도하게 차지하지 않게 한다.
- desktop에서는 정보 밀도를 높이고 mobile에서는 제목, preview, metadata가 자연스럽게 세로 배치되게 한다.

## 12. 검색 영역 크기와 균형

- 검색 input, 검색 button, 범위 탭, 작품 유형, 장르, 자유게시판 분류, 정렬 control의 높이를 공통 규격으로 맞춘다.
- 검색 input이 무조건 전체 viewport를 차지하고 작은 filter가 오른쪽 끝에 흩어지는 구조를 피한다.
- 넓은 desktop에서는 검색 input에 합리적인 flex 비율을 주고 filters를 같은 toolbar 안에 정렬한다.
- 중간 폭에서는 filter가 자연스럽게 다음 줄로 wrap되도록 한다.
- mobile에서는 input을 한 줄 전체 폭으로, button과 filters를 터치 가능한 크기로 배치한다.
- label과 control 사이 간격, card padding, 페이지 최대 폭을 기존 Slate 디자인 토큰과 맞춘다.
- 특정 해상도 pixel 값에만 맞추는 임시 CSS를 피한다.

## DB 및 backend

- `sql/01_schema.sql`에 신규 설치 schema를 반영하고 기존 DB용 멱등 migration SQL을 `sql/`에 추가한다.
- 장르 관계가 새로 필요하면 FK, unique key, 검색 index를 포함한다.
- migration 재실행 시 중복 컬럼, table, index, FK 오류가 없어야 한다.
- 기존 WORK 데이터는 장르 null/빈 배열 상태에서도 정상 조회되어야 한다.
- 게시글 목록/search API는 scope/category, keyword, sort, workType, genreId, freeCategory를 명시적으로 검증한다.
- 검색 결과 route가 호출하는 API에도 기존 공개 범위와 관리자/작성자 접근 정책을 유지한다.
- 주별·월별·전체 ranking query의 날짜 경계와 동률 순서를 테스트한다.
- ranking 응답에 `postId`, title, likeCount, 대표 이미지, YouTube 썸네일, 장르, 작품 유형 등 UI에 필요한 실제 필드를 제공한다.
- frontend가 title이나 index로 상세 ID와 장르를 추측하지 않게 한다.

## 필수 테스트

### Backend

1. WORK/FREE 범위별 keyword 검색과 공개 범위 검증.
2. 작품 유형과 장르 단독/조합 filter 검증.
3. 작업물 장르 등록, 조회, 수정, 삭제/교체 왕복 검증.
4. 주별 경계 안팎 게시글이 정확히 포함/제외됨.
5. 월별 경계 안팎 게시글이 정확히 포함/제외됨.
6. 전체 순위에 기간 제한이 없음.
7. 세 기간 모두 좋아요 수와 동률 규칙대로 정렬됨.
8. 기간별 limit 5와 인기 프로필 limit 10 적용.
9. 목록/검색/랭킹 응답의 대표 이미지, YouTube 썸네일, 장르 필드 계약 일치.
10. migration 신규 적용과 재실행 성공.

### Frontend 및 브라우저

1. 게시판 범위가 select가 아닌 `작업물/자유 게시판` 가로 탭으로 표시.
2. 탭 active 상태와 keyboard/focus 동작 정상.
3. HOME 검색 후 `/boards/search` 또는 합의한 결과 route로 이동하고 새로고침 시 복원.
4. 검색 영역에 초기화 버튼이 없음.
5. HOME heading과 데이터가 `최신 작업물` 최신순으로 일치.
6. 직접 대표 이미지, YouTube 썸네일, 장르 default, 공통 default 우선순위 확인.
7. 작품 유형과 장르 filter가 실제 API 결과를 변경.
8. 사용자 노출 명칭이 `작품 유형`으로 통일.
9. WORK 화면에 일반 글쓰기 버튼이 없고 작업물 올리기는 정상.
10. FREE 화면의 글쓰기 action은 정상.
11. 인기 화면 desktop 좌우 2열, mobile 1열.
12. 인기 작업물 5개, 인기 프로필 10개 이하 표시.
13. 주별·월별·전체 전환 결과가 backend 좋아요 순위와 일치.
14. 자유게시판 목록에서 비정상 줄바꿈, 과도한 빈 공간, 전체 폭 보기 버튼이 없음.
15. desktop, 중간 폭, 390x844에서 검색 toolbar와 filter overflow 없음.
16. console error, 중복 요청, 이전 검색 응답 덮어쓰기 없음.

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
- 실제 DB 데이터로 검색, 장르 filter, 기간별 ranking API 응답 확인
- 브라우저 Network에서 route query와 API의 scope/category/keyword/sort/workType/genreId/ranking period 확인
- `git diff --check`

## 금지사항

- 첫 번째와 두 번째 잘못된 UI 이미지를 목표 레이아웃으로 복제 금지
- 목표 스타일 이미지의 `추천 팀`, `저장한 팀` 문구를 게시판 탭에 복사 금지
- 검색 결과를 같은 HOME 내부 panel만 바꾸고 페이지 이동을 완료 처리 금지
- 초기화 버튼을 CSS로만 숨기고 접근 가능한 DOM에 남겨두기 금지
- 장르 filter를 frontend 배열에만 적용 금지
- team genre를 work genre로 추측해 사용 금지
- 없는 장르 이미지를 샘플 작품 이미지로 위장 금지
- 주별/월별 순위를 frontend 날짜 필터로 계산 금지
- 인기 순위에 댓글, 조회수, reactionScore 사용 금지
- 실제 DB 결과가 없을 때 샘플 게시글·프로필·순위 표시 금지
- 기존 대표 이미지 업로드, YouTube preview, 자유게시판 분류, 좋아요, follow, 상세 route 삭제 금지
- 관련 없는 리팩터링, 사용자 변경 되돌리기, 새 라이브러리 설치, commit/push 금지
- 실제 secret, JWT, API key, DB 비밀번호 출력·문서화 금지
- `../prototype*` 수정 금지

## 완료 보고

`docu/work_logs/YYYY-MM-DD_fixer_board_search_ui_period_ranking.md`에 다음을 기록하세요.

- 12개 요구사항별 수정 결과
- 게시판 범위 탭과 검색 결과 route 최종 계약
- 최신 작업물 조회 기준
- 작품 유형·장르 저장 및 filter 계약
- 이미지 fallback 우선순위와 아직 준비되지 않은 장르 asset 처리
- WORK/FREE별 action 노출 규칙
- 주별·월별·전체 ranking SQL과 날짜/동률 기준
- 인기 작업물·프로필 limit 및 반응형 배치
- 자유게시판과 검색 toolbar UI 변경
- schema/migration/API 변경
- 변경 파일 목록
- test/build/SQL/API/브라우저 검증 결과
- 미수행 검증과 남은 위험
```
