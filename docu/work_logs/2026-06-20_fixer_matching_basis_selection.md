# 팀원 매칭 기준 팀·모집 역할 선택 결함 수정

## 작업 목적

- 팀원 찾기에서 기준 팀과 모집 역할을 반복 변경할 수 있게 한다.
- 필터 초기화 시 기준 팀, 역할, 추가 필터, 결과, URL query를 모두 비우고 자동 복원을 막는다.
- 화면 선택값, URL `teamId`/`slotId`, 일반 매칭 요청 기준을 동일하게 유지한다.

## 읽은 문서와 코드

- `Agent.md`
- `docu/00_common/reference_policy.md`
- `docu/03_mvp_scope/mvp_decisions.md`
- `docu/06_frontend/frontend_baseline.md`
- `docu/work_logs/2026-06-20_frontend_matching_algorithm_filters.md`
- `docu/work_logs/2026-06-20_fixer_matching_member_filters.md`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/services/api.js`의 모집·일반 매칭·AI API 계약
- `frontend/src/router/index.js`의 matching route
- `frontend/src/styles/slate.css`의 matching 관련 스타일

## 재현한 원인

- `loadRecruitments()`가 호출 목적과 관계없이 `route.query.slotId`를 읽어 사용자 변경 직전의 역할을 다시 선택할 수 있었다.
- 팀 변경 시 URL 동기화 전에 모집 역할을 불러와 이전 query가 새 팀 상태를 덮을 수 있었다.
- `resetFilters()`가 추가 필터만 비운 뒤 `applyFilters()`를 호출해 팀·역할과 결과를 유지했다.
- 단일 `suppressNextRouteLoad` boolean은 연속 route 변경에서 다른 변경이 소비할 가능성이 있었다.

## 변경 파일

- `frontend/src/views/MatchingView.vue`
- `docu/work_logs/2026-06-20_fixer_matching_basis_selection.md`

## 구현 내용

- `loadRecruitments()`에 대상 teamId, 복원할 preferredSlotId, 첫 역할 자동 선택 여부를 명시적으로 전달한다.
- 최초 진입과 유효 직접 URL에서만 기존처럼 첫 팀·역할을 복원한다.
- 사용자 팀 변경 시 이전 역할 목록, 역할 선택, 일반 결과, 상세 선택, AI 상태를 먼저 폐기하고 URL의 이전 `slotId`를 제거한다.
- 사용자 팀 변경과 초기화 후에는 역할을 자동 선택하지 않으며 역할 선택 시점에 일반 매칭을 실행한다.
- 단일 팀 계정도 초기화 후 `이 팀을 기준으로 선택` 버튼으로 명시적으로 다시 선택할 수 있다.
- query 억제는 정규화된 query 서명 Set으로 처리해 내부 replace와 외부 route 변경을 구분한다.
- 팀·역할 상호작용 ID, 모집 요청 ID, 일반 매칭 요청 ID, 전체 load 요청 ID로 늦은 응답을 무시한다.

## 최초 복원과 사용자 초기화 구분

- 컴포넌트 내부 `basisSelectionCleared` 상태로 사용자의 명시적 초기화를 기록한다.
- 유효한 직접 URL query가 있으면 복원하고, query가 없는 최초 진입은 기존 기본 선택 UX를 유지한다.
- 초기화 직후 내부 query replace는 watcher 재로딩을 억제하고, 팀을 다시 선택하기 전까지 기본값을 넣지 않는다.

## 팀·역할·query 동기화

- 팀 변경 직후 `selectedSlotId`와 모집 목록을 비운 상태로 query를 먼저 동기화한다.
- 역할 변경 후 최신 선택을 query에 반영한 다음 같은 `selectedTeamId`/`selectedSlotId`로 일반 매칭을 호출한다.
- 숫자 select 값과 문자열 query 값은 `Number(...)`로 비교한다.

## 경쟁 상태 방어

- 빠른 팀 전환은 `basisChangeRequestId`와 `recruitmentRequestId`로 마지막 팀의 모집 결과만 반영한다.
- 빠른 역할 전환은 `basisChangeRequestId`와 `refreshRequestId`로 마지막 역할의 일반 결과만 반영한다.
- 팀·역할 변경과 초기화는 진행 중인 일반/AI 요청 ID를 즉시 무효화한다.

## 검증

- `npm run build`: PASS (`vite v8.0.13`, 92 modules)
- `git diff --check`: PASS
- 브라우저, 팀장 계정:
  - 역할 `slotId=1 -> 2 -> 1` 반복 변경과 select/URL 일치 확인
  - 장르 적용 후 `teamId=1&slotId=1&genreIds=1` 확인
  - 초기화 후 팀·역할·장르·결과·URL query 초기화 확인
  - 초기화 후 500ms 대기해도 팀·역할이 자동 선택되지 않음 확인
  - 단일 팀 재선택 후 URL은 `teamId=1`만 포함하고 결과 요청 전 상태 유지 확인
  - 역할 재선택 후 `teamId=1&slotId=3`, 결과 카드 2개 확인
  - 유효 query 새로고침에서 `slotId=3` 복원 확인
  - 잘못된 `teamId=999&slotId=999`가 관리 가능한 `teamId=1&slotId=1`로 안전하게 정규화됨 확인
  - 관리 팀 0개 계정의 팀 생성 CTA와 기준 재선택 안내 확인
  - 390x844, 768x1024에서 가로 overflow 0 확인
  - console error/warning 0건

## 미검증과 남은 위험

- 현재 계정/데이터에는 한 사용자가 관리하는 팀이 2개 이상인 사례가 없어 A팀 -> B팀 -> A팀 실제 조작은 수행하지 못했다. 코드의 즉시 폐기와 request ID 분기로 검증했다.
- OPEN 역할 0개 팀은 실제 계정으로 확인하지 못했다.
- 실제 네트워크 패널의 query payload는 브라우저 도구에서 직접 캡처하지 못했으며, select/URL과 해당 역할 결과 응답으로 확인했다.
- AI 요청 자체는 외부 호출을 추가 발생시키지 않기 위해 실행하지 않았고, 팀·역할 변경 시 AI request ID와 화면 상태를 초기화하는 코드 경로를 확인했다.
- 저장, 초대, 지원, 팔로우 mutation은 실행하지 않았다.
- Vite의 기존 500kB 초과 chunk 경고는 남아 있으며 이번 변경 범위와 무관하다.

## 변경 범위 확인

- 이번 작업에서 backend 변경 0개
- 이번 작업에서 SQL 변경 0개
- 작업 시작 전 존재하던 backend/SQL 사용자 변경은 그대로 보존
