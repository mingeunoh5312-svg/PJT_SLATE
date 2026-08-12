# 홈 화면 재설계 생성자 프롬프트

## 사용 목적

로그인 일반 사용자 홈을 단순 콘텐츠 나열 화면에서 `오늘의 제작 대시보드`로 재설계한다. 생성 시안의 전체 구조를 참고하되, 사용자 확정 의견에 따라 추천 인물 영역은 모집 포지션으로, 영상 영감 영역은 게시판 최신 글로 교체한다.

## 프롬프트

```text
당신은 Slate 프로젝트의 홈 화면 재설계 생성자입니다. 분석이나 제안만 하지 말고 현재 구현을 읽은 뒤 코드 수정, 실제 데이터 연결, 반응형 UI, 검증까지 완료하세요.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

시각 참고 이미지:
- docu/prompt/home_redesign_reference.png

참고 이미지의 역할:
- 로그인 USER 데스크톱 홈의 정보 위계, 밀도, 2열 배치, 카드 스타일을 설명하는 시안입니다.
- 이미지 속 인명, 프로젝트명, 날짜, 적합도, 게시물은 예시이므로 하드코딩하지 마세요.
- 이미지의 `나를 위한 협업 추천`은 구현하지 않습니다. 그 자리는 `지금 모집 중인 포지션`으로 변경합니다.
- 이미지의 `새로운 영감`은 구현하지 않습니다. 그 자리는 `커뮤니티 새 글`로 변경합니다.
- 시안을 픽셀 단위로 복제하기보다 현재 Slate 레이아웃과 실제 데이터 구조에 맞게 구현하세요.

먼저 읽을 파일:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/03_mvp_scope/mvp_decisions.md
4. docu/06_frontend/frontend_baseline.md
5. docu/prompt/home_dashboard_01_data_structure_prompt.md
6. docu/prompt/home_dashboard_02_ui_responsive_prompt.md
7. docu/work_logs/2026-06-19_home_dashboard_data.md
8. docu/work_logs/2026-06-19_home_dashboard_ui.md
9. frontend/src/views/HomeView.vue
10. frontend/src/styles/slate.css
11. frontend/src/layouts/AppLayout.vue
12. frontend/src/services/api.js
13. frontend/src/router/index.js
14. frontend/src/views/TeamsView.vue
15. frontend/src/views/BoardView.vue
16. frontend/src/views/ContestView.vue
17. backend/src/main/java/com/slate/teams/TeamController.java
18. backend/src/main/java/com/slate/teams/TeamService.java
19. backend/src/main/resources/mappers/TeamMapper.xml

현재 작업 트리에는 사용자의 다른 변경이 있을 수 있습니다. 관련 없는 변경을 되돌리거나 정리하지 말고 홈 재설계에 필요한 범위만 수정하세요.

## 핵심 목표

홈의 우선 흐름을 다음과 같이 만드세요.

1. 개인화된 축소 히어로
2. `오늘 해야 할 일` + `지금 모집 중인 포지션`
3. `진행 중인 프로젝트`
4. `나에게 맞는 공모전` + `커뮤니티 새 글`

로그인 USER가 홈에 진입했을 때 첫 화면에서 다음 두 질문에 바로 답할 수 있어야 합니다.

- 오늘 내가 처리해야 할 일은 무엇인가?
- 지금 참여할 수 있는 새로운 제작 기회는 무엇인가?

## 확정 정보 구조

### 1. 개인화된 축소 히어로

- 기존 `frontend/src/assets/home/hero-set.png`를 재사용합니다.
- 기존 홈보다 세로 높이를 줄여 아래 핵심 영역 일부가 첫 화면에 보이게 합니다.
- 제목 예시: `{displayName 또는 nickname}님, 오늘도 좋은 작품을 만들어볼까요?`
- 보조 문구에는 실제 초대 수나 마감 일정 수를 활용합니다.
- 주 CTA는 `팀 찾기`이며 `/matching/teams`로 이동합니다.
- 보조 CTA가 필요하면 최대 1개만 사용합니다.
- 큰 통계 카드 4개를 히어로 아래에 다시 나열하지 마세요.

### 2. 첫 번째 2열 핵심 영역

데스크톱에서는 좌우 비중이 비슷한 2열, 태블릿과 모바일에서는 1열로 배치합니다.

#### 왼쪽: 오늘 해야 할 일

현재 HomeView의 실제 사용자 활동 데이터를 재사용합니다.

- PENDING 팀 초대
- 현재 사용자에게 배정된 마감 임박 또는 기한 초과 일정
- 읽지 않은 최근 알림 중 이동 가능한 중요 항목
- 최대 3~5개
- 긴급도 badge, 제목, 짧은 설명, 시각 또는 D-day, 명확한 CTA
- route가 없는 알림에는 이동 버튼을 만들지 않습니다.
- 항목이 없으면 `오늘 처리할 긴급한 일이 없습니다.`와 팀 탐색 CTA를 표시합니다.

#### 오른쪽: 지금 모집 중인 포지션

이 영역은 AI 추천, 사용자 추천, 적합도 점수 영역이 아닙니다. 현재 OPEN 상태인 실제 팀 모집 슬롯을 최신 또는 마감 임박 순으로 보여주는 제작 기회 영역입니다.

각 항목에 가능한 실제 필드만 표시합니다.

- 팀명
- 모집 공고 제목
- 모집 역할명
- 지역 또는 지역 무관 여부
- 작업 시작일 또는 예상 기간
- 모집 마감일/D-day
- 필요 인원 또는 잔여 인원
- 팀 또는 모집 상세로 이동하는 `상세 보기`

정책:

- 마감됐거나 CLOSED인 모집 공고/슬롯은 제외합니다.
- 정원이 찬 슬롯은 제외하거나 `모집 완료`로 비활성 표시합니다.
- 현재 사용자가 이미 참여한 팀이나 이미 지원한 슬롯의 처리 기준은 기존 팀 정책을 확인해 중복 지원이 발생하지 않게 합니다.
- 홈에서는 직접 지원 mutation을 새로 만들기보다 모집 상세로 이동시키는 것을 우선합니다.
- 최대 2~3개만 노출하고 `모집 포지션 더보기`는 기존 팀 탐색 또는 매칭 팀 목록 route로 연결합니다.
- 임의 적합도 퍼센트와 임의 추천 사유를 표시하지 않습니다.

현재 프런트에는 팀별 `slateApi.recruitments(teamId)`만 있고 전체 OPEN 모집 포지션을 조회하는 전용 API는 없을 수 있습니다. 다음 순서로 처리하세요.

1. 현재 backend와 matching 응답만으로 정확한 OPEN 모집 포지션 목록을 만들 수 있는지 먼저 확인합니다.
2. 만들 수 없다면 팀 모집 도메인에 읽기 전용 목록 API를 최소 범위로 추가합니다.
3. 새 API는 기존 TeamService/TeamMapper의 상태, 공개 범위, 슬롯 구조를 재사용하고 N+1 조회를 만들지 않습니다.
4. API 경로와 응답 필드는 기존 naming convention을 따릅니다. 예: `GET /api/teams/recruitments/open?limit=3`와 같은 형태를 검토하되 실제 코드 구조에 맞게 확정합니다.
5. API를 추가하면 controller/service/mapper 테스트 또는 최소한 관련 서비스 테스트를 보강합니다.
6. 프런트 `slateApi`에 명시적인 함수를 추가하고 HomeView에서 독립 loading/error/empty 상태로 호출합니다.

### 3. 진행 중인 프로젝트

현재 `myTeams()`와 팀 일정 데이터를 이용해 로그인 사용자가 실제 참여 중인 팀을 최대 2개 보여줍니다.

- 프로젝트/팀 이름
- 현재 팀 상태 또는 제작 단계
- 다음 일정 1건
- 사용자 담당 일정이 있으면 우선 표시
- 완료/전체 일정 수로 계산 가능한 경우에만 진행률 표시
- 계산 근거가 없으면 임의의 60%, 75% 같은 숫자를 만들지 말고 상태 badge로 대체
- `프로젝트 보기`로 `/teams/:teamId` 이동
- 프로젝트가 없으면 팀 탐색 또는 팀 만들기 CTA 제공

### 4. 하단 탐색 영역

데스크톱에서는 `나에게 맞는 공모전`과 `커뮤니티 새 글`을 2열로 배치합니다. 콘텐츠 밀도와 화면 폭에 따라 각 영역 내부는 카드 또는 목록으로 구성할 수 있습니다.

#### 왼쪽: 나에게 맞는 공모전

- 기존 공모전 API와 실제 fit 정렬/필드를 우선 재사용합니다.
- USER에게는 적합도 또는 저장 여부를 활용하되 backend 응답에 존재하는 값만 표시합니다.
- 최대 3개
- 대표 이미지, 제목, 주최, D-day, 마감일, 실제 적합도 badge 또는 관련 역할/장르
- 저장 토글과 상세 이동은 기존 동작을 유지합니다.
- 적합도 데이터가 없을 때 임의로 `높은 적합도`를 만들지 말고 `마감 임박` 또는 실제 장르/역할 badge로 대체합니다.

#### 오른쪽: 커뮤니티 새 글

기존 `새로운 영감`, `최근 등록된 작업물` 섹션을 제거하고 게시판의 실제 최신 글 목록으로 교체합니다.

- 기본 데이터: `slateApi.boardPosts('FREE', 'latest', 4)`
- 제목: `커뮤니티 새 글`
- 최대 4~5개
- 게시글 제목
- 작성자 nickname
- 작성 시각
- 댓글 또는 리뷰 수, 좋아요 수, 조회수 중 실제 응답에 있는 값
- 카테고리 badge가 필요하면 실제 category만 사용
- 전체 행 또는 명시적 CTA로 `/boards/:postId` 이동
- `전체 보기`는 `/boards`로 이동
- 팀 시스템의 공식 모집 포지션과 중복되지 않도록 홈 커뮤니티 영역은 FREE 게시글을 우선합니다.
- 영상 썸네일 중심의 `새로운 영감` 레이아웃은 사용하지 않습니다.

## 계정별 처리

이번 재설계의 핵심 대상은 로그인 `USER`입니다.

- GUEST: 현재 공개 랜딩 흐름과 가입 CTA를 유지하되 깨진 레이아웃이나 제거된 데이터 참조가 없도록 정리합니다.
- COMPANY: 기존 회사 CTA와 공개 콘텐츠를 유지합니다. USER 전용 할 일, 프로젝트, 모집 포지션을 억지로 표시하지 않습니다.
- ADMIN: 기존 관리자 CTA를 유지합니다. USER 전용 데이터를 호출하지 않습니다.
- 로그인/로그아웃 또는 사용자 전환 시 이전 USER 데이터가 남지 않아야 합니다.

## 시각 디자인 기준

- 현재 Slate의 AppLayout, 좌측 사이드바, 상단 바는 유지합니다.
- 배경은 흰색과 아주 옅은 cool gray, 주요 색은 기존 cobalt blue를 사용합니다.
- 카드 radius, border, shadow, typography는 기존 `slate.css` 디자인 언어를 따릅니다.
- 참고 이미지처럼 정보 위계가 분명하고 여백이 충분한 제작 SaaS 대시보드로 만듭니다.
- 과도한 gradient, 장식 도형, 큰 아이콘, 의미 없는 영문 eyebrow를 줄입니다.
- 홈 전용 CSS는 범위를 명확히 하며 관련 없는 전역 스타일을 대규모로 변경하지 않습니다.
- 단순 wrapper를 모두 컴포넌트로 쪼개지 않습니다. HomeView가 지나치게 커질 때만 `components/home` 아래 의미 있는 섹션 단위로 분리합니다.

## 상태와 접근성

모든 네트워크 섹션에 독립 상태를 둡니다.

- loading: 실제 카드 크기에 가까운 skeleton
- error: 해당 섹션 안에서 오류 메시지와 재시도
- empty: 다음 행동을 제시하는 빈 상태
- 한 API 실패가 홈 전체를 막지 않음
- 로딩 중 샘플 데이터를 실제 데이터처럼 표시하지 않음
- 카드 전체 링크 안에 버튼을 중첩하지 않음
- icon-only 버튼에 aria-label 제공
- heading 순서를 지키고 색상만으로 긴급도를 구분하지 않음
- 키보드 focus 상태를 유지

## 반응형 완료 기준

- 데스크톱: 2열 핵심 영역과 2열 하단 영역, 프로젝트 카드 2개
- 태블릿: 섹션별 1열 또는 내용이 보존되는 2열
- 모바일 390x844: 모든 주요 영역 1열, document 가로 overflow 0
- 모바일에서는 히어로 이미지를 축소하거나 숨길 수 있지만 오늘 할 일과 모집 포지션이 먼저 보이게 함
- 긴 제목, 긴 팀명, D-day, 버튼이 카드 밖으로 넘치지 않음
- 사이드바, 하단 탭, 알림 패널과 충돌하지 않음

## 금지 사항

- 참고 이미지의 인명, 팀명, 프로젝트명, 날짜, 퍼센트를 하드코딩
- `나를 위한 협업 추천`, 추천 사용자 카드, AI 적합도 영역 구현
- `새로운 영감` 영상 썸네일 영역 유지
- 모집 포지션을 FREE 게시글에서 추측해 공식 모집처럼 표현
- 실제 근거 없는 프로젝트 진행률이나 공모전 적합도 생성
- 임의 샘플 데이터를 API 결과처럼 표시
- 기존 팔로우, 매칭, 팀, 게시판, 공모전 기능 회귀
- 외부 UI 라이브러리 또는 전역 상태 라이브러리 추가
- 관련 없는 파일 정리, 기존 사용자 변경 revert
- 사용자 지시 없는 commit, push, PR

## 구현 순서

1. 현재 HomeView 데이터 흐름과 최근 변경 diff를 확인합니다.
2. 참고 이미지와 이 프롬프트의 차이를 목록화합니다.
3. 공식 OPEN 모집 포지션을 제공할 수 있는 기존 API 여부를 확인합니다.
4. 필요한 경우 최소 조회 API와 테스트를 먼저 구현합니다.
5. 프런트 API와 HomeView 데이터 상태를 연결합니다.
6. 확정 정보 구조에 맞게 template과 CSS를 수정합니다.
7. 계정별 분기와 loading/error/empty를 확인합니다.
8. 빌드와 테스트를 수행합니다.
9. 가능하면 실제 브라우저에서 데스크톱, 태블릿, 390x844 화면과 route를 검증합니다.

## 필수 검증

Backend를 수정한 경우:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run build
```

브라우저 시나리오:

- USER 데이터 있음: 할 일, OPEN 모집 포지션, 참여 프로젝트, 공모전, FREE 최신 글 확인
- USER 데이터 없음: 각 독립 empty 상태 확인
- 모집 마감/정원 완료 항목이 노출되지 않는지 확인
- 커뮤니티 행이 정확한 게시글 상세로 이동하는지 확인
- 공모전 저장 토글 회귀 확인
- GUEST/COMPANY/ADMIN에서 USER 전용 API를 호출하지 않는지 확인
- 로그아웃 후 개인 데이터가 즉시 제거되는지 확인
- desktop/tablet/390x844에서 overflow와 console error 확인

## 완료 조건 및 보고

- 참고 이미지의 핵심 구조가 현재 Slate 코드에 맞게 구현됨
- `나를 위한 협업 추천`이 `지금 모집 중인 포지션`으로 교체됨
- `새로운 영감`이 `커뮤니티 새 글`로 교체됨
- 모든 표시 값이 실제 API 또는 명시적인 빈 상태에서 옴
- USER 홈의 행동 우선순위가 `오늘 할 일 → 모집 기회 → 진행 프로젝트 → 공모전/커뮤니티`로 읽힘
- backend 수정 시 테스트 통과, frontend build 통과
- 수행하지 못한 브라우저 검증은 완료로 쓰지 않고 이유를 기록

작업 후 `docu/work_logs/YYYY-MM-DD_home_redesign_creator.md`를 작성하세요. 다음 내용을 포함합니다.

- 변경 파일
- 사용하거나 추가한 API와 응답 필드
- 모집 포지션 필터/정렬 기준
- 게시판 조회 기준
- 계정별 동작
- 실행한 테스트와 빌드 결과
- 브라우저 및 반응형 검증 결과
- 미완료 항목과 남은 위험
```

## 참조 파일

- `docu/prompt/home_redesign_reference.png`
- `frontend/src/views/HomeView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/services/api.js`
- `backend/src/main/java/com/slate/teams`
- `backend/src/main/resources/mappers/TeamMapper.xml`
