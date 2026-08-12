# 매칭 탐색 구조와 AI 추천 통합 작업 기록

## 변경 배경

- 기존 매칭 화면은 `시작`, `팀원 찾기`, `팀 찾기`, `AI 추천` 네 탭으로 목적과 추천 방식이 같은 수준에 섞여 있었다.
- `/matching`의 중간 안내 카드와 독립 `/matching/ai` 화면 때문에 실제 탐색까지 한 단계가 추가됐다.
- AI 요청은 별도 화면에 있었고, 느린 이전 요청이 목적 또는 조건 변경 후 상태를 덮는 방어가 없었다.

## 수정 파일

- `frontend/src/router/index.js`
- `frontend/src/views/MatchingView.vue`
- `frontend/src/styles/slate.css`
- `frontend/src/layouts/AppLayout.vue`
- `docu/work_logs/2026-06-20_matching_navigation_ai_integration.md`

## 적용 내용

- `/matching`을 기본 목적지인 `/matching/teams`로 redirect했다.
- 상단 내비게이션을 `팀원 찾기`, `팀 찾기` 두 링크로 정리하고 목록과 상세에서 현재 목적을 유지했다.
- redirect 이후에도 사이드바와 모바일 하단의 매칭 메뉴가 두 목적 경로에서 활성 상태를 유지하도록 섹션 판정을 보완했다.
- 기존 `/matching/ai`는 `mode`와 유효한 `teamId`, `slotId`를 해석해 각 목록의 `view=ai` 상태로 호환 이동하도록 유지했다.
- 각 목록의 필터 영역에 목적별 AI 추천 버튼을 배치하고, 버튼을 눌렀을 때만 기존 AI 추천 API를 호출한다.
- AI 패널은 같은 목록 경로의 `view=ai` 상태에서 표시하며 일반 결과 복귀 동작을 제공한다.
- 팀 또는 모집 역할 변경 시 이전 AI 결과를 초기화하고 일반 결과 query로 복귀한다.
- AI 결과, 오류, carousel index를 일반 매칭 상태와 분리하고 request id 및 unmount 검사를 추가했다.
- 직접 `view=ai`로 접근한 경우 자동 요청하지 않고 실행 준비 상태를 표시한다.

## 반응형·접근성 기준

- 목적 내비게이션은 링크 의미를 사용하고 두 칸 그리드 및 최소 44px 높이를 유지한다.
- 모바일 필터 영역에서 일반 필터와 AI 버튼을 한 행의 유연한 두 칸으로 배치하고 안내 문구는 전체 폭을 사용한다.
- AI 패널에 `aria-busy`를 적용하고 disabled 이유를 연결된 화면 문구로 제공한다.
- 상세 전용 그리드 선택자의 우선순위를 명확히 해 중간 폭 화면에서 일반 2열 규칙에 덮이지 않도록 했다.

## 검증

- `cd frontend && npm run build`: 통과 (`vite v8.0.13`, 92 modules transformed).
- `/matching` 직접 접근이 브라우저 기록에 중간 화면을 렌더링하지 않고 `/matching/teams`로 이동하는 것을 확인했다.
- `/matching/ai?mode=members&teamId=1&slotId=2`가 `/matching/members?view=ai&teamId=1&slotId=2`로 이동하고 자동 추천 요청 없이 준비 상태를 표시하는 것을 확인했다.
- query가 없는 `/matching/ai`가 `/matching/teams` 일반 결과로 이동하는 것을 확인했다.
- 팀 및 팀원 AI 버튼을 명시적으로 누른 뒤에만 `view=ai`가 추가되고, 로컬 backend 응답의 빈 결과 상태가 일반 결과와 구분되어 표시되는 것을 확인했다.
- AI 상태에서 모집 역할을 변경하면 `view=ai`와 이전 AI 상태가 제거되고 일반 목록이 다시 표시되는 것을 확인했다.
- 390x844에서 두 목적 탭과 필터/AI 버튼이 각각 한 줄에 유지됐고 `scrollWidth`와 `clientWidth`가 390px로 일치했다.
- 1024x768 후보 상세에서 AI 패널이 렌더링되지 않고 목적 탭 및 공통 매칭 메뉴 활성 상태가 유지되며 가로 overflow가 없음을 확인했다.
- 확인한 흐름에서 브라우저 console error가 없었다.

## 남은 제약 또는 미확인 사항

- 로컬 backend의 AI 추천 endpoint는 정상 응답했지만 현재 조건에서는 추천 결과가 비어 있어 carousel의 복수 결과 전환과 외부 AI 제공자 응답 내용은 확인하지 못했다.
- backend 계약과 fallback 구현은 변경하지 않았다.
