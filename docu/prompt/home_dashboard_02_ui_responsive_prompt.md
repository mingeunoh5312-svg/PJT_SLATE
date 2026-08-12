# 홈 대시보드 2단계: UI·반응형 구현 프롬프트

## 사용 목적

1단계에서 완성한 실제 데이터 흐름을 유지하면서 로그인 USER 대시보드와 비로그인 랜딩 홈을 SLATE 디자인 시스템에 맞게 구현한다.

## 프롬프트

```text
당신은 Slate 홈 대시보드 2단계 UI 구현 담당자입니다.

선행 조건:
- `docu/prompt/home_dashboard_01_data_structure_prompt.md` 작업이 완료되어 있어야 합니다.
- HomeView의 공개/USER/COMPANY/ADMIN 데이터 분기와 실제 API 로딩이 동작해야 합니다.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 읽을 파일:
1. Agent.md
2. docu/prompt/home_dashboard_01_data_structure_prompt.md
3. 1단계 작업 로그
4. frontend/src/views/HomeView.vue
5. frontend/src/styles/slate.css
6. frontend/src/layouts/AppLayout.vue
7. frontend/src/views/ProfileView.vue
8. frontend/src/views/ContestView.vue
9. frontend/src/views/BoardView.vue

현재 화이트·블루 디자인, 카드 모서리, border, shadow, typography, 사이드바 너비를 유지하세요.
관련 없는 기존 CSS와 사용자 변경을 되돌리지 마세요.

## 최종 정보 구조

### 로그인 USER

1. 개인화된 축소 히어로
2. 내 활동 현황 카드 4개
3. 지금 확인할 활동
4. 마감 임박 공모전
5. 최근 공개 작업물

### 비로그인

1. 서비스 소개 히어로
2. 핵심 기능 3개
3. 마감 임박 공개 공모전
4. 최근 공개 작업물
5. 회원가입 유도 CTA

### COMPANY/ADMIN

- 기존 계정 유형별 주요 CTA 유지
- USER 전용 활동 수치를 표시하지 않음
- 공개 공모전과 최근 작업물을 재사용

## 1. 로그인 히어로

- 기존 `hero-set.png`와 전체 톤은 유지
- 현재보다 높이를 줄여 활동 카드 일부가 첫 화면에 보이게 함
- 제목: `{nickname}님, 오늘의 제작 활동을 확인해보세요`
- 보조 문구는 실제 수치만 사용
- 예: `확인할 초대 1건과 마감 임박 일정 2건이 있어요.`
- 모든 값이 0이면 `새로운 협업을 시작할 준비가 되었어요.`

CTA 최대 3개:

- `팀 찾기` → `/matching/teams`
- `팀 만들기` → `/teams/new`
- `공모전 둘러보기` → `/contests`

추천 결과나 적합도를 히어로에 표시하지 마세요.

## 2. 내 활동 현황

실제 수치 4개:

- 참여 중인 팀
- 받은 팀 초대
- 읽지 않은 알림
- 마감 임박 일정

각 카드는 아이콘, 숫자, 짧은 설명, 이동 동작을 갖습니다.
0도 숨기지 않되 과도한 경고색을 사용하지 마세요.
초대, 미확인 알림, 기한 초과 일정처럼 행동이 필요한 값만 강조합니다.

## 3. 지금 확인할 활동

- 추천 콘텐츠 영역이 아니라 사용자가 처리할 실제 항목 목록
- 초대, 일정, 알림의 유형 badge
- 제목과 한 줄 설명
- 발생 시각 또는 D-day
- 실제 route가 있을 때만 CTA 표시
- 최대 5개 우선 노출
- 항목이 없으면 `지금 확인할 긴급한 활동이 없습니다.` 빈 상태와 팀 탐색 CTA 제공

## 4. 마감 임박 공모전

카드 필드:

- representativeImageUrl
- title
- organizer
- targetText
- requiredRolesText 또는 relatedGenresText
- prizeText
- dDay
- savedByCurrentUser
- 상세 버튼

D-7 이하는 강한 강조, 그 외는 중립 badge를 사용합니다.
이미지가 없으면 기존 로컬 contest asset 또는 단색 placeholder를 사용하되 데이터와 무관한 특정 포스터를 잘못 매핑하지 마세요.
로그인 USER에게만 실제 저장 토글을 제공하고 기존 `toggleContestSave` API를 재사용하세요.
비로그인은 상세 보기만 제공하세요.

## 5. 최근 공개 작업물

- 기존 홈의 `최근 업데이트된 프로젝트` 명칭을 `최근 등록된 작업물`로 변경
- 프로젝트 모집 카드처럼 보이지 않게 함
- youtubeThumbnailUrl 우선
- 썸네일이 없으면 중립 placeholder
- title, authorNickname, workTeamName, mediaType, createdAt
- likeCount, reviewCount, viewCount 중 공간에 맞는 실제 반응 정보
- 전체 카드 또는 명시적 CTA로 `/boards/:postId` 이동

## 6. 비로그인 랜딩 홈

히어로:

- `함께 만들 다음 작품을 찾아보세요`
- `무료로 시작하기` → `/register`
- `로그인` → `/login`
- `작업물 둘러보기` → `/boards`

핵심 기능 3개:

- 역할과 조건을 기반으로 팀·팀원 매칭
- 영화 제작 팀과 모집 관리
- 작품에 맞는 공모전 탐색

가짜 사용자 수, 가짜 성공률, 가짜 적합도는 표시하지 마세요.

## 컴포넌트 정책

HomeView가 지나치게 커질 경우 다음처럼 의미 있는 섹션만 분리할 수 있습니다.

- `components/home/HomeHero.vue`
- `components/home/HomeActivitySummary.vue`
- `components/home/HomeActionList.vue`
- `components/home/HomeContestSection.vue`
- `components/home/HomeWorkSection.vue`

단순 wrapper까지 모두 컴포넌트로 쪼개지 말고, props와 emit이 명확한 섹션만 분리하세요.

## 상태 UI

- loading: 실제 카드 크기에 가까운 skeleton
- error: 섹션 안에서 메시지와 재시도 제공
- empty: 다음 행동을 안내
- 전체 화면 spinner 하나로 모든 섹션을 막지 않음
- 로딩 중 임의 샘플 카드를 보여주지 않음

## 반응형

- 데스크톱: 활동 카드 4열, 콘텐츠 2~3열
- 태블릿: 활동 카드 2열, 콘텐츠 2열
- 모바일: 1열, 필요한 경우 작업물만 가로 스크롤 허용
- 히어로 CTA는 모바일에서 자연스럽게 줄바꿈
- D-day와 버튼이 카드 밖으로 넘치지 않게 함
- 390px 폭에서 document 가로 overflow가 없어야 함

## 접근성

- 카드 전체가 이동 요소이면 중첩 interactive element 금지
- 북마크와 카드 링크 클릭 충돌 방지
- icon-only 버튼에 aria-label
- heading 순서 유지
- 색상만으로 긴급도 구분하지 않음
- skeleton과 오류 상태에 적절한 상태 텍스트 제공

## 금지 사항

- 1단계 API 계산 로직을 임의로 목업으로 대체
- 추천 프로젝트·추천 팀원 섹션 추가
- 팔로우 활동 피드 추가
- 기존 팔로우 화면 수정
- 백엔드/SQL 수정
- 외부 UI 라이브러리 추가
- 현재 디자인과 무관한 전면 리브랜딩
- 관련 없는 전역 CSS 대규모 변경

## 검증

- `npm run build`
- 비로그인/USER/COMPANY/ADMIN 데스크톱 확인
- USER 데이터 있음/없음 상태 확인
- API 섹션별 loading/error/empty 확인
- 모바일 390x844 및 태블릿 폭 확인
- console error 0건 확인
- 모든 CTA와 상세 route 확인

작업 후 `docu/work_logs/YYYY-MM-DD_home_dashboard_ui.md`를 작성하세요.
```

