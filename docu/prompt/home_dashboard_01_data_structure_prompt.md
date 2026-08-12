# 홈 대시보드 1단계: 데이터·상태 구조 구현 프롬프트

## 사용 목적

기존 하드코딩 홈을 실제 API 기반으로 전환하고, 비로그인·일반 사용자·회사·관리자 상태를 안전하게 분리한다. 이 단계에서는 디자인을 완성하지 않고 데이터 흐름과 동작 가능한 기본 구조를 먼저 만든다.

## 프롬프트

```text
당신은 Slate 홈 대시보드 1단계 구현 담당자입니다.

작업 루트:
- /Users/mingeunoh/Documents/SSAFY/PJT_Final/Project_Slate/Slate

먼저 읽을 파일:
1. Agent.md
2. docu/00_common/reference_policy.md
3. docu/06_frontend/frontend_baseline.md
4. frontend/src/views/HomeView.vue
5. frontend/src/services/api.js
6. frontend/src/router/index.js
7. frontend/src/App.vue
8. frontend/src/layouts/AppLayout.vue
9. frontend/src/views/TeamsView.vue
10. frontend/src/views/ContestView.vue
11. frontend/src/views/BoardView.vue

현재 사용자 변경 사항을 보존하고 관련 없는 파일을 되돌리거나 정리하지 마세요.

## 목표

`HomeView.vue`의 하드코딩 추천 프로필, 공모전, 프로젝트 배열과 임의 수치를 제거하고 실제 API 데이터로 홈 상태를 구성합니다.

홈 상태는 다음 네 가지로 분리합니다.

1. 비로그인: 공개 랜딩 홈
2. 로그인 USER: 개인화 제작 대시보드
3. 로그인 COMPANY: 회사 계정용 기존 주요 CTA와 공개 콘텐츠
4. 로그인 ADMIN: 관리자 CTA와 공개 콘텐츠

팔로우 기능은 기존 매칭·프로필 화면에서 유지합니다. 아직 존재하지 않는 팔로우 활동 피드는 홈에 만들지 마세요.

## 사용할 기존 API

공개 데이터:

- `slateApi.contests({ status: 'OPEN', sort: 'deadline', limit: 6 })`
- `slateApi.boardPosts('WORK', 'latest', 4)`

로그인 USER 데이터:

- `slateApi.myProfile()`
- `slateApi.myTeams()`
- `slateApi.myTeamInvitations()`
- `slateApi.unreadNotifications()`
- `slateApi.notifications({ unreadOnly: true, limit: 5 })`
- `slateApi.teamPlans(teamId)`

필요한 경우 현재 `api.js`의 실제 함수 시그니처를 기준으로 인자를 조정하세요. 백엔드에 없는 홈 전용 API를 가정하지 마세요.

## 데이터 로딩 정책

- 공개 공모전과 작업물은 모든 계정 상태에서 조회할 수 있습니다.
- 인증 전용 API는 `currentUser?.accountType === 'USER'`일 때만 호출합니다.
- USER 데이터는 공개 데이터와 독립적으로 로드합니다.
- 여러 독립 요청은 `Promise.allSettled`를 사용하여 하나의 실패가 홈 전체를 막지 않게 합니다.
- `myTeams()` 결과를 받은 뒤 참여 팀에 대해서만 `teamPlans(teamId)`를 조회합니다.
- 사용자 변경, 로그인, 로그아웃이 발생하면 이전 개인화 상태를 먼저 초기화합니다.
- 느린 이전 요청이 새 사용자의 상태를 덮지 않도록 request id 등의 경쟁 상태 방어를 적용합니다.
- 컴포넌트 unmount 이후 상태를 갱신하지 않도록 정리합니다.

## 실제 수치 계산

USER 홈 요약 수치는 다음 기준으로 계산합니다.

### 참여 중인 팀

- `myTeams()` 중 현재 활동 상태인 팀
- 우선 상태: `RECRUITING`, `IN_PROGRESS`, `RECRUITMENT_CLOSED`, `CLOSING`
- 종료·삭제 팀은 제외

### 받은 팀 초대

- `myTeamInvitations()` 중 `status === 'PENDING'`

### 읽지 않은 알림

- `unreadNotifications().unreadCount`

### 마감 임박 일정

- 각 참여 팀의 `teamPlans(teamId)` 결과
- 현재 사용자에게 배정된 항목: `assigneeUserId === currentUser.userId`
- `DONE`, `CANCELED` 제외
- `dueAt`이 현재부터 7일 이내
- 이미 마감된 항목은 별도의 overdue 표시가 가능하지만 총계에서 누락하지 마세요.

날짜 계산은 로컬 시간대에서 날짜 경계를 안정적으로 처리하고 invalid date는 제외하세요.

## 홈 표시용 데이터 가공

### 마감 임박 공모전

- OPEN 공모전만 사용
- dDay가 0 이상인 항목
- 조회 결과 안에서 `savedByCurrentUser` 우선, 다음 dDay 오름차순
- 최대 3개
- 비로그인에서는 저장 여부를 개인화 정보처럼 강조하지 않음

### 최근 공개 작업물

- WORK/latest 응답 최대 4개
- 실제 `postId`, `title`, `authorNickname`, `workTeamName`, `mediaType`, `youtubeThumbnailUrl`, `createdAt`, 반응 수 사용
- 프로젝트 모집 데이터처럼 표현하지 않음

### 지금 확인할 활동

로그인 USER에 한해 다음 실제 데이터로 구성합니다.

- PENDING 팀 초대
- 마감 또는 기한 초과 일정
- 읽지 않은 최근 알림

각 항목에는 type, title, description, occurredAt 또는 dueAt, target route를 갖는 화면용 객체를 만드세요.
지원하지 않는 targetType을 억지로 잘못된 상세 route와 연결하지 마세요.

권장 route:

- 팀/초대/팀 일정 → `/teams/:teamId` 또는 `/teams`
- 공모전 → `/contests/:contestId`
- 게시물 → `/boards/:postId`
- 이동할 수 없는 일반 알림 → 버튼 없이 표시

## 기본 화면 구조

이번 단계에서는 최종 시각 디자인보다 분기와 데이터 정확성을 우선합니다.

- guest home
  - 소개 히어로
  - 공개 공모전
  - 최근 공개 작업물
- USER dashboard
  - 개인화 히어로
  - 내 활동 요약
  - 지금 확인할 활동
  - 마감 임박 공모전
  - 최근 공개 작업물
- COMPANY/ADMIN
  - 계정별 기존 CTA
  - 공개 공모전
  - 최근 공개 작업물

각 섹션에 독립 loading, error, empty 상태를 둡니다.

## 금지 사항

- 백엔드, SQL, SecurityConfig 수정
- 추천 프로젝트·추천 팀원·적합도 영역 추가
- 팔로우 활동 피드 추가
- 임의 사용자, 임의 숫자, 임의 공모전, 임의 프로젝트 생성
- 기존 팔로우 프런트 기능 변경
- 공모전 지역처럼 API에 없는 필드 표시
- 로그인하지 않은 상태에서 인증 API 호출
- 전역 상태 관리 라이브러리 도입
- 이번 단계에서 CSS 대규모 재작성

## 검증

1. `npm run build`
2. 비로그인 상태에서 인증 API가 호출되지 않는지 확인
3. USER 로그인 후 실제 팀·초대·알림·일정 수치 확인
4. COMPANY/ADMIN에서 USER API가 호출되지 않는지 확인
5. API 하나를 실패시켜도 다른 섹션이 남는지 확인
6. 로그아웃 후 이전 USER 수치가 남지 않는지 확인
7. 하드코딩된 기존 홈 추천 데이터가 제거됐는지 확인

## 완료 조건

- 실제 API 기반 상태와 계산 로직 구현
- 계정 상태별 분기 구현
- 섹션별 loading/error/empty 상태 구현
- 기존 하드코딩 홈 데이터 제거
- 추천 매칭 및 팔로우 피드 미포함
- build 성공

작업 후 `docu/work_logs/YYYY-MM-DD_home_dashboard_data.md`를 작성하고 변경 파일, API, 계산 기준, 검증 결과를 기록하세요.
```

